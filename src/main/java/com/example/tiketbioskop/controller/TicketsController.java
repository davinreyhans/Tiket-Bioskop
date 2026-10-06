package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.entity.Tickets;
import com.example.tiketbioskop.usecase.ticket.TicketRequest;
import com.example.tiketbioskop.usecase.ticket.TicketsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsUseCase ticketsUseCase;

    // Public: seats still free for a schedule
    @GetMapping("/schedules/{scheduleId}/seats")
    public List<String> getAvailableSeats(@PathVariable Integer scheduleId) {
        return ticketsUseCase.getAvailableSeats(scheduleId);
    }

    // One or more seats: {"scheduleId":1,"seatsCodes":["A1","A2"]}
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Tickets> bookTickets(Principal principal, @Valid @RequestBody TicketRequest request) {
        return ticketsUseCase.bookTickets(principal.getName(), request);
    }

    @GetMapping("/tickets/me")
    public List<Tickets> getMyTickets(Principal principal) {
        return ticketsUseCase.getMyTickets(principal.getName());
    }

    // Owner or admin, up to 2 hours before the show
    @DeleteMapping("/tickets/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelTicket(@PathVariable Integer ticketId, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        ticketsUseCase.cancelTicket(ticketId, authentication.getName(), isAdmin);
    }
}
