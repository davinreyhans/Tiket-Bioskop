package com.example.tiketbioskop.usecase.booking;

import com.example.tiketbioskop.entity.Bookings;
import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.entity.Tickets;
import com.example.tiketbioskop.entity.Users;
import com.example.tiketbioskop.exception.ConflictException;
import com.example.tiketbioskop.exception.NotFoundException;
import com.example.tiketbioskop.repository.DaoBookings;
import com.example.tiketbioskop.repository.DaoTickets;
import com.example.tiketbioskop.usecase.schedule.SchedulesUseCase;
import com.example.tiketbioskop.usecase.ticket.TicketsUseCase;
import com.example.tiketbioskop.usecase.users.UsersUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingsUseCase {

    // how long reserved seats wait for payment
    public static final Duration RESERVATION_TIME = Duration.ofMinutes(5);

    private final DaoBookings daoBookings;

    private final DaoTickets daoTickets;

    private final SchedulesUseCase schedulesUseCase;

    private final TicketsUseCase ticketsUseCase;

    private final UsersUseCase usersUseCase;

    // local cinema time (see TimeConfig)
    private final Clock clock;

    // Reserves the seats for 5 minutes (never past the start of the show). All seats or none:
    // any failure rolls the whole request back. The unique (schedule_id, seats_code) constraint
    // is the real guard against two people getting the same seat.
    @Transactional
    public BookingView reserve(String username, BookingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Schedules schedule = schedulesUseCase.getSchedulesByScheduleId(request.scheduleId());
        if (!now.isBefore(schedule.startsAt())) {
            throw new ConflictException("Jadwal ini sudah mulai, tiket tidak bisa dipesan lagi.");
        }
        // sorted: concurrent bookings then lock overlapping seats in the same order (no deadlock)
        List<String> wanted = request.seatsCodes().stream().distinct().sorted(TicketsUseCase.SEAT_ORDER).toList();

        Set<String> studioSeats = ticketsUseCase.studioSeats(schedule);
        List<String> unknown = wanted.stream().filter(code -> !studioSeats.contains(code)).toList();
        if (!unknown.isEmpty()) {
            throw new NotFoundException("Kursi " + String.join(", ", unknown) + " tidak ada di studio " + schedule.getStudioName() + ".");
        }

        // expired reservations still own their rows; clear them so the unique constraint lets the seats go
        daoBookings.deleteExpired(schedule, now);
        Set<String> held = ticketsUseCase.heldSeats(schedule.getScheduleId());
        List<String> taken = wanted.stream().filter(held::contains).toList();
        if (!taken.isEmpty()) {
            throw new ConflictException("Kursi " + String.join(", ", taken) + " baru saja dipesan orang lain. Pilih kursi lain.");
        }

        Users user = usersUseCase.getUserByUsername(username);
        Bookings booking = new Bookings();
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setStatus(Bookings.RESERVED);
        booking.setCreatedAt(now);
        LocalDateTime reservedUntil = now.plus(RESERVATION_TIME);
        booking.setExpiresAt(reservedUntil.isBefore(schedule.startsAt()) ? reservedUntil : schedule.startsAt());
        daoBookings.save(booking);

        booking.setTickets(daoTickets.saveAll(wanted.stream().map(code -> {
            Tickets ticket = new Tickets();
            ticket.setUser(user);
            ticket.setBooking(booking);
            ticket.setSchedule(schedule);
            ticket.setStudioName(schedule.getStudioName());
            ticket.setSeatsCode(code);
            return ticket;
        }).toList()));
        return new BookingView(booking, now);
    }

    // the visitor's reservations that can still be paid
    public List<Bookings> getMyReservations(String username) {
        return daoBookings.findByUserUsernameAndStatusAndExpiresAtAfterOrderByExpiresAt(
                username, Bookings.RESERVED, LocalDateTime.now(clock));
    }

    // Tidies up only: expired reservations already count as free seats everywhere, this just
    // removes their rows (they also stop a schedule or user from being deleted)
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public int deleteExpiredReservations() {
        return daoBookings.deleteAllExpired(LocalDateTime.now(clock));
    }

    // owner or admin
    public BookingView getBooking(Integer bookingId, String username, boolean isAdmin) {
        return new BookingView(ownBooking(bookingId, username, isAdmin), LocalDateTime.now(clock));
    }

    // Simulated payment: no gateway, paying just marks the booking PAID.
    // Idempotent: paying an already paid booking again returns it unchanged.
    @Transactional
    public BookingView pay(Integer bookingId, String username) {
        LocalDateTime now = LocalDateTime.now(clock);
        Bookings booking = ownBooking(bookingId, username, false);
        if (Bookings.RESERVED.equals(booking.getStatus()) && daoBookings.markPaid(bookingId, now) == 0) {
            throw new ConflictException("Waktu pembayaran habis dan kursinya sudah dilepas. Silakan pesan ulang.");
        }
        return new BookingView(ownBooking(bookingId, username, false), now);
    }

    // Unpaid: always, the seats are free right away. Paid: same rule as single tickets,
    // up to 2 hours before the show.
    @Transactional
    public void cancel(Integer bookingId, String username, boolean isAdmin) {
        Bookings booking = ownBooking(bookingId, username, isAdmin);
        LocalDateTime cancelDeadline = booking.getSchedule().startsAt().minus(TicketsUseCase.CANCEL_DEADLINE);
        if (Bookings.PAID.equals(booking.getStatus()) && LocalDateTime.now(clock).isAfter(cancelDeadline)) {
            throw new ConflictException("Tiket hanya bisa dibatalkan paling lambat 2 jam sebelum film mulai.");
        }
        daoBookings.delete(booking);
    }

    // an expired reservation may already be gone (cleared by a later booking of its seats)
    private Bookings ownBooking(Integer bookingId, String username, boolean isAdmin) {
        Bookings booking = daoBookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Pesanan tidak ditemukan atau waktu pembayarannya sudah habis."));
        if (!isAdmin && !booking.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ini bukan pesananmu.");
        }
        return booking;
    }
}
