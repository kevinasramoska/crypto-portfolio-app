package com.kevinas.crypto_portfolio_backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JwtServiceTest {

    @Test
    void validateConfiguration_shouldAcceptASufficientSecretAndPositiveExpiry() {
        JwtService jwtService = jwtServiceWith("a-32-byte-or-longer-secret-for-tests", 3600);

        assertThatCode(jwtService::validateConfiguration).doesNotThrowAnyException();
    }

    @Test
    void validateConfiguration_shouldRejectShortSecrets() {
        JwtService jwtService = jwtServiceWith("too-short", 3600);

        assertThatIllegalStateException()
                .isThrownBy(jwtService::validateConfiguration)
                .withMessage("JWT secret must be at least 32 bytes");
    }

    @Test
    void validateConfiguration_shouldRejectNonPositiveExpiry() {
        JwtService jwtService = jwtServiceWith("a-32-byte-or-longer-secret-for-tests", 0);

        assertThatIllegalStateException()
                .isThrownBy(jwtService::validateConfiguration)
                .withMessage("JWT expiration must be positive");
    }

    private JwtService jwtServiceWith(String secret, long expirationSeconds) {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expirationSeconds", expirationSeconds);
        return jwtService;
    }
}
