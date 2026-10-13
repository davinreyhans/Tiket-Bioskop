package com.example.tiketbioskop.usecase.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotNull(message = "Jadwal wajib dipilih.") Integer scheduleId,
        @NotEmpty(message = "Pilih minimal 1 kursi.") @Size(max = 10, message = "Maksimal 10 kursi per pesanan.")
        List<@NotBlank(message = "Kode kursi wajib diisi.") String> seatsCodes) {
}
