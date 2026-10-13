package com.example.tiketbioskop.usecase.booking;

import com.example.tiketbioskop.entity.Bookings;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.time.LocalDateTime;

// The booking plus the server's clock (local cinema time): the payment countdown is
// expiresAt - serverTime, so a wrong clock on the visitor's device doesn't matter
public record BookingView(@JsonUnwrapped Bookings booking, LocalDateTime serverTime) {
}
