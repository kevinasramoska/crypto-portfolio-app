package com.kevinas.crypto_portfolio_backend.controller;

import com.kevinas.crypto_portfolio_backend.dto.JwtResponse;
import com.kevinas.crypto_portfolio_backend.dto.LoginRequest;
import com.kevinas.crypto_portfolio_backend.dto.RegisterRequest;
import com.kevinas.crypto_portfolio_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}