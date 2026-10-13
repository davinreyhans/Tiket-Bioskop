package com.example.tiketbioskop.usecase.film;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FilmRequest(
        @NotBlank(message = "Kode film wajib diisi.") @Size(max = 20, message = "Kode film maksimal 20 karakter.") String filmCode,
        @NotBlank(message = "Judul film wajib diisi.") @Size(max = 255, message = "Judul film maksimal 255 karakter.") String filmName,
        @NotNull(message = "Status tayang wajib diisi.") Boolean isShowing) {
}
