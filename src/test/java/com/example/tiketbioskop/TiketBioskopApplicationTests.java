package com.example.tiketbioskop;

import com.example.tiketbioskop.repository.DaoUsers;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

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
        mvc.perform(options("/tickets").header(HttpHeaders.ORIGIN, "https://frontend.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.example"));
        mvc.perform(options("/tickets").header(HttpHeaders.ORIGIN, "https://evil.example")
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

        postJson("/tickets", "carol", tickets(scheduleId, "A1", "A2"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)));
        postJson("/tickets", "carol", tickets(scheduleId, "A3", "A1")).andExpect(status().isConflict());
        postJson("/tickets", "carol", tickets(scheduleId, "Z9")).andExpect(status().isNotFound());
        postJson("/tickets", "carol", "{\"scheduleId\":" + scheduleId + ",\"seatsCodes\":[]}")
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
        postJson("/tickets", "carol", tickets(pastId, "A1")).andExpect(status().isConflict());

        Integer lateId = id(postJson("/schedules", "admin", schedule(filmId, "A", FUTURE, "22:00", "23:30"))
                .andExpect(status().isCreated()), "$.scheduleId");
        postJson("/tickets", "carol", tickets(lateId, "A1")).andExpect(status().isCreated());
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
                .andExpect(status().isConflict()).andExpect(conflictMessage("still has schedules"));
        Integer unusedFilm = id(postJson("/films", "admin", film("F-002")), "$.filmId");
        send(HttpMethod.DELETE, "/films/" + unusedFilm, "admin", null).andExpect(status().isNoContent());

        send(HttpMethod.DELETE, "/users/" + userId("carol"), "admin", null)
                .andExpect(status().isConflict()).andExpect(conflictMessage("still has tickets"));
        send(HttpMethod.DELETE, "/users/" + userId("dave"), "admin", null).andExpect(status().isNoContent());
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
