package com.example.tiketbioskop.usecase.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Username wajib diisi.") @Size(max = 50, message = "Username maksimal 50 karakter.") String username,
        @NotBlank(message = "Email wajib diisi.") @Email(message = "Format email tidak valid.")
        @Size(max = 255, message = "Email maksimal 255 karakter.") String email,
        @NotBlank(message = "Password wajib diisi.") @Size(min = 8, max = 72, message = "Password harus 8–72 karakter.") String password) { // BCrypt only hashes the first 72 bytes
}
