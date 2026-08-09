package com.kevinas.crypto_portfolio_backend.integration;

import com.kevinas.crypto_portfolio_backend.config.TestConfig;
import com.kevinas.crypto_portfolio_backend.dto.SupportedCoinResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rate-limit.auth.max-requests=2",
        "rate-limit.auth.window-seconds=60",
        "rate-limit.market.max-requests=2",
        "rate-limit.market.window-seconds=60"
})
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
class RateLimitingIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authRequests_shouldBeLimitedPerClientAddress() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(mockRemoteAddress("203.0.113.10"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(mockRemoteAddress("203.0.113.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too many requests. Please try again later."));

        mockMvc.perform(post("/api/auth/login")
                        .with(mockRemoteAddress("203.0.113.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void marketRequests_shouldBeLimitedPerClientAddress() throws Exception {
        when(marketDataService.getSupportedCoins()).thenReturn(List.of(
                new SupportedCoinResponse("BTC", "Bitcoin", "bitcoin")
        ));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(get("/api/market/supported-coins")
                            .with(mockRemoteAddress("198.51.100.10")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/market/supported-coins")
                        .with(mockRemoteAddress("198.51.100.10")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        mockMvc.perform(get("/api/market/supported-coins")
                        .with(mockRemoteAddress("198.51.100.11")))
                .andExpect(status().isOk());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor mockRemoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
