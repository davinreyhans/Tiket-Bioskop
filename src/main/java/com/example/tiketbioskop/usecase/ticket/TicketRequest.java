package com.example.tiketbioskop.usecase.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TicketRequest(
        @NotNull Integer scheduleId,
        @NotEmpty @Size(max = 10) List<@NotBlank String> seatsCodes) {
}
