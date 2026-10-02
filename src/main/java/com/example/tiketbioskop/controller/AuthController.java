package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.usecase.auth.AuthUseCase;
import com.example.tiketbioskop.usecase.auth.LoginRequest;
import com.example.tiketbioskop.usecase.auth.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;

    // Send the token back as "Authorization: Bearer <token>"
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authUseCase.login(request);
    }
}
