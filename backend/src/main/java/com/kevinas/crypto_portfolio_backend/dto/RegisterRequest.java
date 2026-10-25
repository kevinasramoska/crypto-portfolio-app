package com.kevinas.crypto_portfolio_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
    public RegisterRequest {
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
