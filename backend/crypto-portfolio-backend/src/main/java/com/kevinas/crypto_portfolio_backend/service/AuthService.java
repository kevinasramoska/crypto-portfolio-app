package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.JwtResponse;
import com.kevinas.crypto_portfolio_backend.dto.LoginRequest;
import com.kevinas.crypto_portfolio_backend.dto.RegisterRequest;

public interface AuthService {
    JwtResponse register(RegisterRequest request);
    JwtResponse login(LoginRequest request);
}