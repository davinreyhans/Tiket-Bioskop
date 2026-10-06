package com.example.tiketbioskop.usecase.auth;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt) {
}
