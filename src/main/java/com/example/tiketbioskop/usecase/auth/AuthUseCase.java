package com.example.tiketbioskop.usecase.auth;

import com.example.tiketbioskop.entity.Users;
import com.example.tiketbioskop.repository.DaoUsers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthUseCase {

    // ponytail: one token, no refresh/revocation; a deleted or demoted user keeps access until it expires
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final DaoUsers daoUsers;

    private final PasswordEncoder passwordEncoder;

    private final JwtEncoder jwtEncoder;

    public TokenResponse login(LoginRequest request) {
        Users users = daoUsers.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                // same message for unknown user and wrong password, so usernames can't be probed
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(users.getUsername())
                .claim("roles", users.getRole())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, claims.getExpiresAt());
    }
}
