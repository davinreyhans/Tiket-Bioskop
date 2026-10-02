package com.example.tiketbioskop.usecase.film;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FilmRequest(
        @NotBlank @Size(max = 20) String filmCode,
        @NotBlank @Size(max = 255) String filmName,
        @NotNull Boolean isShowing) {
}
