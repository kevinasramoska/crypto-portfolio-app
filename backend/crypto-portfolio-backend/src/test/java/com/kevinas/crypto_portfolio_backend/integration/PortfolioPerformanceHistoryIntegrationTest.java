package com.kevinas.crypto_portfolio_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.dto.JwtResponse;
import com.kevinas.crypto_portfolio_backend.dto.LoginRequest;
import com.kevinas.crypto_portfolio_backend.dto.RegisterRequest;
import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.model.PortfolioSnapshot;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioPerformanceHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    @MockBean
    private MarketDataService marketDataService;

    private String getJwtToken(String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JwtResponse jwtResponse = objectMapper.readValue(response, JwtResponse.class);
        return jwtResponse.accessToken();
    }

    @Test
    void performanceHistory_shouldReturnEmptyArray_whenNoSnapshotsExist() throws Exception {
        String token = getJwtToken("emptyhistory@example.com", "password");

        mockMvc.perform(get("/api/portfolio/performance/history")
                        .param("range", "7d")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("7d"))
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    void transactionCreation_shouldPersistSnapshot_andHistoryShouldReturnIt() throws Exception {
        String token = getJwtToken("snapshotpersist@example.com", "password");

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("60000.00"));

        TransactionRequest buy = new TransactionRequest(
                "BTC",
                "Bitcoin",
                TransactionType.BUY,
                new BigDecimal("1.00000000"),
                new BigDecimal("50000.00")
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buy)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolio/performance/history")
                        .param("range", "7d")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].totalInvestedUsd").value(50000.00))
                .andExpect(jsonPath("$.history[0].totalCurrentValueUsd").value(60000.00))
                .andExpect(jsonPath("$.history[0].totalProfitLossUsd").value(10000.00));
    }


    @Test
    void transactionShouldSucceed_whenSnapshotPriceLookupFails() throws Exception {
        String token = getJwtToken("snapshotfallback@example.com", "password");

        doThrow(new RuntimeException("market unavailable"))
                .when(marketDataService)
                .getCurrentPrice("BTC");

        TransactionRequest buy = new TransactionRequest(
                "BTC",
                "Bitcoin",
                TransactionType.BUY,
                new BigDecimal("1.00000000"),
                new BigDecimal("50000.00")
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buy)))
                .andExpect(status().isOk());
    }

    @Test
    void performanceHistory_shouldReturnSnapshotsInAscendingOrder() throws Exception {
        String email = "orderedhistory@example.com";
        String token = getJwtToken(email, "password");
        User user = userRepository.findByEmail(email).orElseThrow();

        portfolioSnapshotRepository.save(PortfolioSnapshot.builder()
                .user(user)
                .snapshotAt(Instant.now().minusSeconds(600))
                .totalInvestedUsd(new BigDecimal("200.00"))
                .totalCurrentValueUsd(new BigDecimal("210.00"))
                .totalProfitLossUsd(new BigDecimal("10.00"))
                .build());

        portfolioSnapshotRepository.save(PortfolioSnapshot.builder()
                .user(user)
                .snapshotAt(Instant.now().minusSeconds(1200))
                .totalInvestedUsd(new BigDecimal("100.00"))
                .totalCurrentValueUsd(new BigDecimal("95.00"))
                .totalProfitLossUsd(new BigDecimal("-5.00"))
                .build());

        mockMvc.perform(get("/api/portfolio/performance/history")
                        .param("range", "7d")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0].totalInvestedUsd").value(100.00))
                .andExpect(jsonPath("$.history[1].totalInvestedUsd").value(200.00));
    }


    @Test
    void performanceHistory_shouldRejectUnsupportedRange() throws Exception {
        String token = getJwtToken("invalidrange@example.com", "password");

        mockMvc.perform(get("/api/portfolio/performance/history")
                        .param("range", "1y")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void performanceHistory_shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/portfolio/performance/history")
                        .param("range", "7d"))
                .andExpect(status().isForbidden());
    }
}
