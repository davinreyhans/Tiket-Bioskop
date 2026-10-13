package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.entity.Bookings;
import com.example.tiketbioskop.usecase.booking.BookingRequest;
import com.example.tiketbioskop.usecase.booking.BookingView;
import com.example.tiketbioskop.usecase.booking.BookingsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingsController {
    private final BookingsUseCase bookingsUseCase;

    // Reserves one or more seats for 5 minutes: {"scheduleId":1,"seatsCodes":["A1","A2"]}
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView reserve(Principal principal, @Valid @RequestBody BookingRequest request) {
        return bookingsUseCase.reserve(principal.getName(), request);
    }

    // Reservations still waiting for payment (paid ones are in /tickets/me)
    @GetMapping("/me")
    public List<Bookings> getMyReservations(Principal principal) {
        return bookingsUseCase.getMyReservations(principal.getName());
    }

    // Owner or admin
    @GetMapping("/{bookingId}")
    public BookingView getBooking(@PathVariable Integer bookingId, Authentication authentication) {
        return bookingsUseCase.getBooking(bookingId, authentication.getName(), isAdmin(authentication));
    }

    // Owner only; simulated payment, safe to repeat
    @PostMapping("/{bookingId}/pay")
    public BookingView pay(@PathVariable Integer bookingId, Principal principal) {
        return bookingsUseCase.pay(bookingId, principal.getName());
    }

    // Owner or admin: unpaid any time, paid up to 2 hours before the show
    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Integer bookingId, Authentication authentication) {
        bookingsUseCase.cancel(bookingId, authentication.getName(), isAdmin(authentication));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
