package com.kevinas.crypto_portfolio_backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProductionSafetyConfig {
    private final Environment environment;

    @PostConstruct
    void validate() {
        String secret = required("jwt.secret");
        if (secret.contains("local-development") || secret.contains("test_jwt") || secret.contains("e2e-jwt")) {
            throw new IllegalStateException("Production must not use a published development signing key");
        }
        String url = required("spring.datasource.url");
        String query = java.net.URI.create(url.substring("jdbc:".length())).getRawQuery();
        java.util.List<String> sslModes = query == null ? java.util.List.of() : java.util.Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8).equals("sslmode"))
                .map(parts -> parts.length == 2 ? java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8) : "")
                .toList();
        if (!url.startsWith("jdbc:postgresql:") || !sslModes.equals(java.util.List.of("verify-full"))
                || url.contains("sslfactory=") || url.contains("sslhostnameverifier=")) {
            throw new IllegalStateException("Production PostgreSQL requires sslmode=verify-full and a trusted certificate");
        }
        required("spring.datasource.username");
        required("spring.datasource.password");
        for (String origin : required("app.cors.allowed-origins").split(",")) {
            java.net.URI uri = java.net.URI.create(origin.trim());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || !uri.getPath().isEmpty()
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalStateException("Production CORS requires explicit HTTPS origins");
            }
        }
    }

    private String required(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing production setting: " + key);
        return value;
    }
}
