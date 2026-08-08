package com.kevinas.crypto_portfolio_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.config.TestConfig;
import com.kevinas.crypto_portfolio_backend.dto.JwtResponse;
import com.kevinas.crypto_portfolio_backend.dto.LoginRequest;
import com.kevinas.crypto_portfolio_backend.dto.RegisterRequest;
import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class, SnapshotFailureTransactionIntegrationTest.SnapshotFailureConfig.class})
@ActiveProfiles("test")
class SnapshotFailureTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    @Test
    void transactionShouldCommit_whenPostCommitSnapshotCreationFails() throws Exception {
        String email = "snapshotfailure@example.com";
        String token = getJwtToken(email, "password");
        TransactionRequest buy = new TransactionRequest(
                "FAIL",
                "Failure Coin",
                TransactionType.BUY,
                new BigDecimal("1.00000000"),
                new BigDecimal("50000.00")
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buy)))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail(email).orElseThrow();
        assertThat(transactionRepository.findByUserOrderByCreatedAtDesc(user)).hasSize(1);
        assertThat(holdingRepository.findByUser(user)).hasSize(1);
        assertThat(portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, Instant.EPOCH))
                .isEmpty();
        verify(portfolioService).createSnapshotForUser(user.getId());
    }

    private String getJwtToken(String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, JwtResponse.class).accessToken();
    }

    @TestConfiguration
    static class SnapshotFailureConfig {

        @Bean
        @Primary
        PortfolioService failingPortfolioService() {
            PortfolioService portfolioService = mock(PortfolioService.class);
            doThrow(new IllegalStateException("snapshot persistence unavailable"))
                    .when(portfolioService)
                    .createSnapshotForUser(anyLong());
            return portfolioService;
        }
    }
}
