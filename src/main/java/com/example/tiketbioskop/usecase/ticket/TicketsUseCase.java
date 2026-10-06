package com.example.tiketbioskop.usecase.ticket;

import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.entity.Seats;
import com.example.tiketbioskop.entity.Tickets;
import com.example.tiketbioskop.entity.Users;
import com.example.tiketbioskop.exception.ConflictException;
import com.example.tiketbioskop.exception.NotFoundException;
import com.example.tiketbioskop.repository.DaoSeats;
import com.example.tiketbioskop.repository.DaoTickets;
import com.example.tiketbioskop.usecase.schedule.SchedulesUseCase;
import com.example.tiketbioskop.usecase.users.UsersUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketsUseCase {

    // Seat codes are "<row letter><number>", e.g. A1..E10
    private static final Comparator<String> SEAT_ORDER = Comparator
            .comparing((String code) -> code.charAt(0))
            .thenComparing(code -> Integer.parseInt(code.substring(1)));

    private static final Duration CANCEL_DEADLINE = Duration.ofHours(2);

    private final DaoTickets daoTickets;

    private final DaoSeats daoSeats;

    private final SchedulesUseCase schedulesUseCase;

    private final UsersUseCase usersUseCase;

    // Schedules store local cinema time, so "now" must be in the cinema's zone, not the server's
    @Value("${bioskop.timezone}")
    private ZoneId zone;

    public List<String> getAvailableSeats(Integer scheduleId) {
        Schedules schedule = schedulesUseCase.getSchedulesByScheduleId(scheduleId);
        Set<String> booked = bookedSeats(scheduleId);
        return studioSeats(schedule).stream()
                .filter(code -> !booked.contains(code))
                .sorted(SEAT_ORDER)
                .toList();
    }

    public List<Tickets> getMyTickets(String username) {
        return daoTickets.findByUserUsernameOrderByTicketId(username);
    }

    // All seats or none: any failure rolls the whole request back.
    // The unique (schedule_id, seats_code) constraint is the real guard against double booking.
    @Transactional
    public List<Tickets> bookTickets(String username, TicketRequest request) {
        Schedules schedule = schedulesUseCase.getSchedulesByScheduleId(request.scheduleId());
        if (!LocalDateTime.now(zone).isBefore(schedule.startsAt())) {
            throw new ConflictException("Schedule id '" + schedule.getScheduleId() + "' has already started.");
        }
        List<String> wanted = request.seatsCodes().stream().distinct().toList();

        Set<String> studioSeats = studioSeats(schedule);
        List<String> unknown = wanted.stream().filter(code -> !studioSeats.contains(code)).toList();
        if (!unknown.isEmpty()) {
            throw new NotFoundException("Seats " + unknown + " not found in studio " + schedule.getStudioName() + ".");
        }
        Set<String> booked = bookedSeats(schedule.getScheduleId());
        List<String> taken = wanted.stream().filter(booked::contains).toList();
        if (!taken.isEmpty()) {
            throw new ConflictException("Seats " + taken + " are already booked.");
        }

        Users user = usersUseCase.getUserByUsername(username);
        return daoTickets.saveAll(wanted.stream().map(code -> {
            Tickets ticket = new Tickets();
            ticket.setUser(user);
            ticket.setSchedule(schedule);
            ticket.setStudioName(schedule.getStudioName());
            ticket.setSeatsCode(code);
            return ticket;
        }).toList());
    }

    @Transactional
    public void cancelTicket(Integer ticketId, String username, boolean isAdmin) {
        Tickets ticket = daoTickets.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket id '" + ticketId + "' not found."));
        if (!isAdmin && !ticket.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own tickets.");
        }
        if (LocalDateTime.now(zone).isAfter(ticket.getSchedule().startsAt().minus(CANCEL_DEADLINE))) {
            throw new ConflictException("Tickets can only be cancelled up to 2 hours before the show starts.");
        }
        daoTickets.delete(ticket);
    }

    private Set<String> studioSeats(Schedules schedule) {
        return daoSeats.findByStudioName(schedule.getStudioName()).stream()
                .map(Seats::getSeatsCode)
                .collect(Collectors.toSet());
    }

    private Set<String> bookedSeats(Integer scheduleId) {
        return daoTickets.findByScheduleScheduleId(scheduleId).stream()
                .map(Tickets::getSeatsCode)
                .collect(Collectors.toSet());
    }
}
