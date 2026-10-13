package com.example.tiketbioskop;

import com.example.tiketbioskop.repository.DaoBookings;
import com.example.tiketbioskop.repository.DaoUsers;
import com.example.tiketbioskop.usecase.booking.BookingsUseCase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TiketBioskopApplicationTests {

    private static final String PASSWORD = "secret123";

    // far from "now", so the 2-hour cancel deadline never depends on when the test runs
    private static final String FUTURE = "2099-01-01";
    private static final String PAST = "2000-01-01";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DaoUsers daoUsers;

    @Autowired
    private DaoBookings daoBookings;

    @Autowired
    private BookingsUseCase bookingsUseCase;

    // frozen at start-up; tests move it forward instead of waiting 5 minutes
    @Autowired
    private MutableClock clock;

    @TestConfiguration
    static class TestTime {
        @Bean
        @Primary
        MutableClock testClock(@Value("${bioskop.timezone}") ZoneId zone) {
            return new MutableClock(zone);
        }
    }

    static class MutableClock extends Clock {
        private final ZoneId zone;
        private volatile Instant instant = Instant.now();

        MutableClock(ZoneId zone) {
            this.zone = zone;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    void registerAndAccessRules() throws Exception {
        register("alice").andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
        register("alice").andExpect(status().isConflict());
        postJson("/users", null, "{\"username\":\"bob\",\"email\":\"not-an-email\",\"password\":\"short\"}")
                .andExpect(status().isBadRequest());

        mvc.perform(get("/films")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());

        // CORS: only the configured frontend origin may call the API from a browser
        mvc.perform(options("/bookings").header(HttpHeaders.ORIGIN, "https://frontend.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.example"));
        mvc.perform(options("/bookings").header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/films").header(HttpHeaders.ORIGIN, "https://frontend.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.example"));
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer("alice")))
                .andExpect(jsonPath("$.username").value("alice"));
        postJson("/films", null, film("F-ALICE")).andExpect(status().isUnauthorized());
        postJson("/auth/login", null, "{\"username\":\"alice\",\"password\":\"wrong-pass\"}")
                .andExpect(status().isUnauthorized());
        postJson("/auth/login", null, "{\"username\":\"nobody\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
        postJson("/films", "alice", film("F-ALICE")).andExpect(status().isForbidden());
    }

    @Test
    void bookingFlow() throws Exception {
        register("admin").andExpect(status().isCreated());
        daoUsers.findByUsername("admin").ifPresent(u -> {
            u.setRole("ADMIN");
            daoUsers.save(u);
        });
        register("carol").andExpect(status().isCreated());
        register("dave").andExpect(status().isCreated());

        Integer filmId = id(postJson("/films", "admin", film("F-001")).andExpect(status().isCreated()), "$.filmId");

        // add schedules + filter
        postJson("/schedules", "admin", schedule(filmId, "A", FUTURE, "21:00", "19:00")).andExpect(status().isBadRequest());
        postJson("/schedules", "admin", schedule(filmId, "Z", FUTURE, "19:00", "21:00")).andExpect(status().isNotFound());
        Integer scheduleId = id(postJson("/schedules", "admin", schedule(filmId, "A", FUTURE, "19:00", "21:00"))
                .andExpect(status().isCreated()), "$.scheduleId");
        Integer matineeId = id(postJson("/schedules", "admin", schedule(filmId, "B", FUTURE, "13:00", "15:00"))
                .andExpect(status().isCreated()), "$.scheduleId");

        mvc.perform(get("/schedules").param("filmId", filmId.toString()))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].filmStartTime").value("13:00:00"));
        mvc.perform(get("/schedules").param("filmId", filmId.toString()).param("date", FUTURE))
                .andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/schedules").param("date", "2099-01-02")).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/schedules").param("filmId", "999999")).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/schedules").param("filmId", filmId.toString()).param("from", FUTURE))
                .andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/schedules").param("filmId", filmId.toString()).param("from", "2099-01-02"))
                .andExpect(jsonPath("$.content", hasSize(0)));

        // pagination: page 2 of 2 holds the evening show; size is capped; unknown sort field = 400
        mvc.perform(get("/schedules").param("filmId", filmId.toString()).param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].filmStartTime").value("19:00:00"))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        mvc.perform(get("/films")).andExpect(jsonPath("$.page.size").value(20));
        mvc.perform(get("/schedules")).andExpect(jsonPath("$.page.size").value(20));
        mvc.perform(get("/films").param("size", "1000")).andExpect(jsonPath("$.page.size").value(100));
        mvc.perform(get("/films").param("sort", "noSuchField")).andExpect(status().isBadRequest());
        mvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, bearer("admin")))
                .andExpect(jsonPath("$.content[0].username").exists())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());

        // overnight shows + no two shows in one studio at the same time
        postJson("/schedules", "admin", schedule(filmId, "A", FUTURE, "19:00", "19:00")).andExpect(status().isBadRequest());
        postJson("/schedules", "admin", schedule(filmId, "A", FUTURE, "20:00", "22:00")).andExpect(status().isConflict());
        postJson("/schedules", "admin", schedule(filmId, "C", FUTURE, "23:00", "01:00")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.filmEndTime").value("01:00:00"));
        postJson("/schedules", "admin", schedule(filmId, "C", "2099-01-02", "00:30", "02:00")).andExpect(status().isConflict());
        postJson("/schedules", "admin", schedule(filmId, "C", FUTURE, "22:00", "23:30")).andExpect(status().isConflict());
        postJson("/schedules", "admin", schedule(filmId, "C", "2099-01-02", "01:00", "03:00")).andExpect(status().isCreated());
        postJson("/schedules", "admin", schedule(filmId, "B", FUTURE, "23:00", "01:00")).andExpect(status().isCreated());
        send(HttpMethod.PUT, "/schedules/" + matineeId, "admin", schedule(filmId, "B", FUTURE, "13:00", "15:30"))
                .andExpect(status().isOk());

        // book several seats at once, all or nothing
        String seats = "/schedules/" + scheduleId + "/seats";
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(50))).andExpect(jsonPath("$[0]").value("A1"))
                .andExpect(jsonPath("$[9]").value("A10"));

        Integer carolsBooking = id(postJson("/bookings", "carol", tickets(scheduleId, "A2", "A1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.tickets", hasSize(2)))
                .andExpect(jsonPath("$.tickets[0].seatsCode").value("A1")), "$.bookingId");
        mvc.perform(get("/tickets/me").header(HttpHeaders.AUTHORIZATION, bearer("carol"))).andExpect(jsonPath("$", hasSize(0)));
        myReservations("carol").andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].bookingId").value(carolsBooking));
        pay("carol", carolsBooking);
        myReservations("carol").andExpect(jsonPath("$", hasSize(0)));
        postJson("/bookings", "carol", tickets(scheduleId, "A3", "A1")).andExpect(status().isConflict());
        postJson("/bookings", "carol", tickets(scheduleId, "Z9")).andExpect(status().isNotFound());
        postJson("/bookings", "carol", "{\"scheduleId\":" + scheduleId + ",\"seatsCodes\":[]}")
                .andExpect(status().isBadRequest());

        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(48)))
                .andExpect(jsonPath("$", not(hasItem("A1")))).andExpect(jsonPath("$", hasItem("A3")));
        mvc.perform(get("/tickets/me").header(HttpHeaders.AUTHORIZATION, bearer("carol")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].schedule.film.filmCode").value("F-001"));

        // cancel: owner or admin only, up to 2 hours before the show
        Integer firstTicket = myFirstTicket("carol");
        send(HttpMethod.DELETE, "/tickets/" + firstTicket, "dave", null).andExpect(status().isForbidden());
        send(HttpMethod.DELETE, "/tickets/" + firstTicket, "carol", null).andExpect(status().isNoContent());
        send(HttpMethod.DELETE, "/tickets/" + firstTicket, "carol", null).andExpect(status().isNotFound());
        send(HttpMethod.DELETE, "/tickets/" + myFirstTicket("carol"), "admin", null).andExpect(status().isNoContent());
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(50)));

        // booking closes when the show starts; a show moved into the past can't be cancelled either
        Integer pastId = id(postJson("/schedules", "admin", schedule(filmId, "A", PAST, "19:00", "21:00"))
                .andExpect(status().isCreated()), "$.scheduleId");
        postJson("/bookings", "carol", tickets(pastId, "A1")).andExpect(status().isConflict());

        Integer lateId = id(postJson("/schedules", "admin", schedule(filmId, "A", FUTURE, "22:00", "23:30"))
                .andExpect(status().isCreated()), "$.scheduleId");
        pay("carol", id(postJson("/bookings", "carol", tickets(lateId, "A1")).andExpect(status().isCreated()), "$.bookingId"));
        send(HttpMethod.PUT, "/schedules/" + lateId, "admin", schedule(filmId, "A", PAST, "10:00", "12:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.filmStartTime").value("10:00:00"));
        send(HttpMethod.DELETE, "/tickets/" + myFirstTicket("carol"), "carol", null).andExpect(status().isConflict());

        // update / delete schedules
        send(HttpMethod.PUT, "/schedules/" + lateId, "admin", schedule(filmId, "B", PAST, "10:00", "12:00"))
                .andExpect(status().isConflict());
        send(HttpMethod.PUT, "/schedules/" + scheduleId, "admin", schedule(filmId, "C", FUTURE, "19:00", "21:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.studioName").value("C"));
        send(HttpMethod.PUT, "/schedules/" + matineeId, "carol", schedule(filmId, "B", FUTURE, "13:00", "15:00"))
                .andExpect(status().isForbidden());
        send(HttpMethod.DELETE, "/schedules/" + lateId, "admin", null).andExpect(status().isConflict());
        send(HttpMethod.DELETE, "/schedules/" + pastId, "admin", null).andExpect(status().isNoContent());
        send(HttpMethod.DELETE, "/schedules/" + matineeId, "admin", null).andExpect(status().isNoContent());
        mvc.perform(get("/schedules/" + matineeId)).andExpect(status().isNotFound());

        // deleting what is still in use says why, instead of the generic "still referenced" 409
        send(HttpMethod.DELETE, "/films/" + filmId, "admin", null)
                .andExpect(status().isConflict()).andExpect(conflictMessage("masih punya jadwal"));
        Integer unusedFilm = id(postJson("/films", "admin", film("F-002")), "$.filmId");
        send(HttpMethod.DELETE, "/films/" + unusedFilm, "admin", null).andExpect(status().isNoContent());

        send(HttpMethod.DELETE, "/users/" + userId("carol"), "admin", null)
                .andExpect(status().isConflict()).andExpect(conflictMessage("masih punya tiket"));
        send(HttpMethod.DELETE, "/users/" + userId("dave"), "admin", null).andExpect(status().isNoContent());
    }

    @Test
    void reservationsExpire() throws Exception {
        register("erin").andExpect(status().isCreated());
        register("frank").andExpect(status().isCreated());
        register("boss").andExpect(status().isCreated());
        daoUsers.findByUsername("boss").ifPresent(u -> {
            u.setRole("ADMIN");
            daoUsers.save(u);
        });
        Integer filmId = id(postJson("/films", "boss", film("F-RSV")).andExpect(status().isCreated()), "$.filmId");
        Integer scheduleId = id(postJson("/schedules", "boss", schedule(filmId, "A", "2099-02-01", "19:00", "21:00"))
                .andExpect(status().isCreated()), "$.scheduleId");
        String seats = "/schedules/" + scheduleId + "/seats";

        // a reservation holds its seats for 5 minutes
        Integer bookingId = id(postJson("/bookings", "erin", tickets(scheduleId, "B1", "B2"))
                .andExpect(status().isCreated()), "$.bookingId");
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(48)));
        myReservations("erin").andExpect(jsonPath("$", hasSize(1)));
        postJson("/bookings", "frank", tickets(scheduleId, "B2", "B3")).andExpect(status().isConflict());

        // past expiresAt the seats are free at once, and the expired booking makes way for a new one
        clock.advance(BookingsUseCase.RESERVATION_TIME);
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(50)));
        myReservations("erin").andExpect(jsonPath("$", hasSize(0)));
        Integer franksBooking = id(postJson("/bookings", "frank", tickets(scheduleId, "B2", "B3"))
                .andExpect(status().isCreated()), "$.bookingId");
        assertThat(daoBookings.findById(bookingId)).isEmpty();
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(48)));

        // cancelling the last seat removes the booking too
        pay("frank", franksBooking);
        send(HttpMethod.DELETE, "/tickets/" + myFirstTicket("frank"), "frank", null).andExpect(status().isNoContent());
        send(HttpMethod.DELETE, "/tickets/" + myFirstTicket("frank"), "frank", null).andExpect(status().isNoContent());
        assertThat(daoBookings.findById(franksBooking)).isEmpty();

        // the cleanup job removes expired reservations nobody has re-booked, and leaves the rest
        Integer forgotten = id(postJson("/bookings", "erin", tickets(scheduleId, "E9")), "$.bookingId");
        Integer active = id(postJson("/bookings", "frank", tickets(scheduleId, "E10")), "$.bookingId");
        clock.advance(Duration.ofMinutes(3));
        Integer fresh = id(postJson("/bookings", "frank", tickets(scheduleId, "E8")), "$.bookingId");
        pay("frank", active);
        clock.advance(Duration.ofMinutes(3));
        assertThat(bookingsUseCase.deleteExpiredReservations()).isPositive();
        assertThat(daoBookings.findById(forgotten)).isEmpty();
        assertThat(daoBookings.findById(active)).isPresent();
        assertThat(daoBookings.findById(fresh)).isPresent();

        // a show starting in less than 5 minutes: the reservation ends when the show starts
        LocalDateTime start = LocalDateTime.now(clock).plusMinutes(3).withSecond(0).withNano(0);
        Integer soonId = id(postJson("/schedules", "boss", schedule(filmId, "B", start.toLocalDate().toString(),
                time(start), time(start.plusHours(1)))).andExpect(status().isCreated()), "$.scheduleId");
        postJson("/bookings", "erin", tickets(soonId, "A1")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").value(start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    @Test
    void payAndCancelBookings() throws Exception {
        register("gina").andExpect(status().isCreated());
        register("hank").andExpect(status().isCreated());
        register("chief").andExpect(status().isCreated());
        daoUsers.findByUsername("chief").ifPresent(u -> {
            u.setRole("ADMIN");
            daoUsers.save(u);
        });
        Integer filmId = id(postJson("/films", "chief", film("F-PAY")).andExpect(status().isCreated()), "$.filmId");
        Integer scheduleId = id(postJson("/schedules", "chief", schedule(filmId, "A", "2099-03-01", "19:00", "21:00"))
                .andExpect(status().isCreated()), "$.scheduleId");
        String seats = "/schedules/" + scheduleId + "/seats";

        // detail: owner or admin, with the server clock for the countdown
        Integer bookingId = id(postJson("/bookings", "gina", tickets(scheduleId, "C1", "C2"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.serverTime").exists()), "$.bookingId");
        String booking = "/bookings/" + bookingId;
        mvc.perform(get(booking).header(HttpHeaders.AUTHORIZATION, bearer("gina"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.serverTime").exists())
                .andExpect(jsonPath("$.tickets", hasSize(2)))
                .andExpect(jsonPath("$.schedule.scheduleId").value(scheduleId));
        mvc.perform(get(booking).header(HttpHeaders.AUTHORIZATION, bearer("hank"))).andExpect(status().isForbidden());
        mvc.perform(get(booking).header(HttpHeaders.AUTHORIZATION, bearer("chief"))).andExpect(status().isOk());
        mvc.perform(get("/bookings/999999").header(HttpHeaders.AUTHORIZATION, bearer("gina"))).andExpect(status().isNotFound());
        mvc.perform(get(booking)).andExpect(status().isUnauthorized());

        // pay: owner only, idempotent, and a paid booking no longer expires
        postJson(booking + "/pay", "hank", null).andExpect(status().isForbidden());
        postJson(booking + "/pay", "chief", null).andExpect(status().isForbidden());
        String paidAt = id(postJson(booking + "/pay", "gina", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID")), "$.paidAt");
        postJson(booking + "/pay", "gina", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.paidAt").value(paidAt));
        clock.advance(BookingsUseCase.RESERVATION_TIME);
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(48)));
        mvc.perform(get("/tickets/me").header(HttpHeaders.AUTHORIZATION, bearer("gina"))).andExpect(jsonPath("$", hasSize(2)));

        // too late to pay: 409 while the expired reservation still exists, 404 once a new booking cleared it
        Integer lateId = id(postJson("/bookings", "hank", tickets(scheduleId, "D1")), "$.bookingId");
        clock.advance(BookingsUseCase.RESERVATION_TIME);
        postJson("/bookings/" + lateId + "/pay", "hank", null)
                .andExpect(status().isConflict()).andExpect(conflictMessage("Waktu pembayaran habis"));
        postJson("/bookings", "gina", tickets(scheduleId, "D1")).andExpect(status().isCreated());
        postJson("/bookings/" + lateId + "/pay", "hank", null).andExpect(status().isNotFound());

        // cancel an unpaid reservation: the seats are free right away
        Integer unpaidId = id(postJson("/bookings", "hank", tickets(scheduleId, "E1", "E2")), "$.bookingId");
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(45)));
        send(HttpMethod.DELETE, "/bookings/" + unpaidId, "gina", null).andExpect(status().isForbidden());
        send(HttpMethod.DELETE, "/bookings/" + unpaidId, "hank", null).andExpect(status().isNoContent());
        send(HttpMethod.DELETE, "/bookings/" + unpaidId, "hank", null).andExpect(status().isNotFound());
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(47)));

        // a paid booking: cancellable up to 2 hours before the show, also by an admin
        send(HttpMethod.DELETE, booking, "chief", null).andExpect(status().isNoContent());
        mvc.perform(get(seats)).andExpect(jsonPath("$", hasSize(49)));
        LocalDateTime soon = LocalDateTime.now(clock).plusMinutes(90).withSecond(0).withNano(0);
        Integer soonId = id(postJson("/schedules", "chief", schedule(filmId, "C", soon.toLocalDate().toString(),
                time(soon), time(soon.plusHours(1)))).andExpect(status().isCreated()), "$.scheduleId");
        Integer soonBooking = id(postJson("/bookings", "gina", tickets(soonId, "A1")), "$.bookingId");
        postJson("/bookings/" + soonBooking + "/pay", "gina", null).andExpect(status().isOk());
        send(HttpMethod.DELETE, "/bookings/" + soonBooking, "gina", null)
                .andExpect(status().isConflict()).andExpect(conflictMessage("2 jam"));
    }

    @Test
    void twoUsersSameSeat() throws Exception {
        register("ivy").andExpect(status().isCreated());
        register("jack").andExpect(status().isCreated());
        register("head").andExpect(status().isCreated());
        daoUsers.findByUsername("head").ifPresent(u -> {
            u.setRole("ADMIN");
            daoUsers.save(u);
        });
        Integer filmId = id(postJson("/films", "head", film("F-RACE")).andExpect(status().isCreated()), "$.filmId");
        Integer scheduleId = id(postJson("/schedules", "head", schedule(filmId, "A", "2099-04-01", "19:00", "21:00"))
                .andExpect(status().isCreated()), "$.scheduleId");
        String ivy = bearer("ivy");
        String jack = bearer("jack");

        // both press "book" on the same seat at the same moment, five times over: one wins each round
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (String seat : List.of("C1", "C2", "C3", "C4", "C5")) {
                CountDownLatch go = new CountDownLatch(1);
                List<Future<Integer>> results = List.of(ivy, jack).stream().map(token -> pool.submit(() -> {
                    go.await();
                    return mvc.perform(request(HttpMethod.POST, "/bookings").contentType(MediaType.APPLICATION_JSON)
                                    .header(HttpHeaders.AUTHORIZATION, token).content(tickets(scheduleId, seat)))
                            .andReturn().getResponse().getStatus();
                })).toList();
                go.countDown();
                assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
            }
        } finally {
            pool.shutdown();
        }
        mvc.perform(get("/schedules/" + scheduleId + "/seats")).andExpect(jsonPath("$", hasSize(45)));
    }

    private void pay(String username, Integer bookingId) throws Exception {
        postJson("/bookings/" + bookingId + "/pay", username, null).andExpect(status().isOk());
    }

    private ResultActions myReservations(String username) throws Exception {
        return mvc.perform(get("/bookings/me").header(HttpHeaders.AUTHORIZATION, bearer(username)));
    }

    private static String time(LocalDateTime dateTime) {
        return dateTime.toLocalTime().toString().substring(0, 5);
    }

    private Integer userId(String username) throws Exception {
        return id(mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(username))), "$.userId");
    }

    private static ResultMatcher conflictMessage(String text) {
        return result -> assertThat(result.getResolvedException()).hasMessageContaining(text);
    }

    private Integer myFirstTicket(String username) throws Exception {
        return id(mvc.perform(get("/tickets/me").header(HttpHeaders.AUTHORIZATION, bearer(username))), "$[0].ticketId");
    }

    private ResultActions register(String username) throws Exception {
        return postJson("/users", null,
                "{\"username\":\"" + username + "\",\"email\":\"" + username + "@mail.com\",\"password\":\"" + PASSWORD + "\"}");
    }

    private ResultActions postJson(String url, String asUser, String body) throws Exception {
        return send(HttpMethod.POST, url, asUser, body);
    }

    private ResultActions send(HttpMethod method, String url, String asUser, String body) throws Exception {
        var request = request(method, url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(body);
        }
        if (asUser != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(asUser));
        }
        return mvc.perform(request);
    }

    private String bearer(String username) throws Exception {
        String token = id(postJson("/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk()), "$.token");
        return "Bearer " + token;
    }

    private static String film(String code) {
        return "{\"filmCode\":\"" + code + "\",\"filmName\":\"Film " + code + "\",\"isShowing\":true}";
    }

    private static String schedule(Integer filmId, String studio, String date, String start, String end) {
        return "{\"filmId\":" + filmId + ",\"studioName\":\"" + studio + "\",\"filmDate\":\"" + date + "\","
                + "\"filmStartTime\":\"" + start + "\",\"filmEndTime\":\"" + end + "\",\"ticketPrice\":50000}";
    }

    private static String tickets(Integer scheduleId, String... seatsCodes) {
        return "{\"scheduleId\":" + scheduleId + ",\"seatsCodes\":[\"" + String.join("\",\"", seatsCodes) + "\"]}";
    }

    private static <T> T id(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
