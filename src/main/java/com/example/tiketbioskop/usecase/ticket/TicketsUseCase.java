package com.example.tiketbioskop.usecase.ticket;

import com.example.tiketbioskop.entity.Bookings;
import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.entity.Seats;
import com.example.tiketbioskop.entity.Tickets;
import com.example.tiketbioskop.exception.ConflictException;
import com.example.tiketbioskop.exception.NotFoundException;
import com.example.tiketbioskop.repository.DaoBookings;
import com.example.tiketbioskop.repository.DaoSeats;
import com.example.tiketbioskop.repository.DaoTickets;
import com.example.tiketbioskop.usecase.schedule.SchedulesUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketsUseCase {

    // Seat codes are "<row letter><number>", e.g. A1..E10
    public static final Comparator<String> SEAT_ORDER = Comparator
            .comparing((String code) -> code.charAt(0))
            .thenComparing(code -> Integer.parseInt(code.substring(1)));

    public static final Duration CANCEL_DEADLINE = Duration.ofHours(2);

    private final DaoTickets daoTickets;

    private final DaoBookings daoBookings;

    private final DaoSeats daoSeats;

    private final SchedulesUseCase schedulesUseCase;

    // local cinema time (see TimeConfig)
    private final Clock clock;

    public List<String> getAvailableSeats(Integer scheduleId) {
        Schedules schedule = schedulesUseCase.getSchedulesByScheduleId(scheduleId);
        Set<String> held = heldSeats(scheduleId);
        return studioSeats(schedule).stream()
                .filter(code -> !held.contains(code))
                .sorted(SEAT_ORDER)
                .toList();
    }

    public List<Tickets> getMyTickets(String username) {
        return daoTickets.findPaidByUsername(username);
    }

    @Transactional
    public void cancelTicket(Integer ticketId, String username, boolean isAdmin) {
        Tickets ticket = daoTickets.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Tiket tidak ditemukan."));
        if (!isAdmin && !ticket.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kamu hanya bisa membatalkan tiketmu sendiri.");
        }
        if (LocalDateTime.now(clock).isAfter(ticket.getSchedule().startsAt().minus(CANCEL_DEADLINE))) {
            throw new ConflictException("Tiket hanya bisa dibatalkan paling lambat 2 jam sebelum film mulai.");
        }
        Bookings booking = ticket.getBooking();
        daoTickets.delete(ticket);
        daoTickets.flush();
        // the last seat of a booking takes the empty booking with it
        if (!daoTickets.existsByBooking(booking)) {
            daoBookings.delete(booking);
        }
    }

    public Set<String> studioSeats(Schedules schedule) {
        return daoSeats.findByStudioName(schedule.getStudioName()).stream()
                .map(Seats::getSeatsCode)
                .collect(Collectors.toSet());
    }

    // seats of paid bookings and of reservations that haven't expired yet
    public Set<String> heldSeats(Integer scheduleId) {
        return Set.copyOf(daoTickets.findHeldSeats(scheduleId, LocalDateTime.now(clock)));
    }
}
