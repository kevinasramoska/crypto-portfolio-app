package com.kevinas.crypto_portfolio_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.dto.*;
        import com.kevinas.crypto_portfolio_backend.model.TransactionType;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void summary_shouldReturnCorrectTotals_withMockedMarketPrices() throws Exception {
        String token = getJwtToken("summaryuser@example.com", "password");

        // Mock prices
        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("60000.00"));
        when(marketDataService.getCurrentPrice("ETH")).thenReturn(new BigDecimal("3000.00"));

        // Buy BTC
        TransactionRequest btcBuy = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("50000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(btcBuy)))
                .andExpect(status().isOk());

        // Buy ETH
        TransactionRequest ethBuy = new TransactionRequest("ETH", "Ethereum", TransactionType.BUY, new BigDecimal("2.00000000"), new BigDecimal("2500.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ethBuy)))
                .andExpect(status().isOk());

        // Sell some BTC
        TransactionRequest btcSell = new TransactionRequest("BTC", "Bitcoin", TransactionType.SELL, new BigDecimal("0.50000000"), new BigDecimal("55000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(btcSell)))
                .andExpect(status().isOk());

        // Get summary
        mockMvc.perform(get("/api/portfolio/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestedUsd").value(30000.00))  // 0.5 BTC * 50000 + 2 ETH * 2500
                .andExpect(jsonPath("$.totalCurrentValueUsd").value(36000.00))  // 0.5 BTC * 60000 + 2 ETH * 3000
                .andExpect(jsonPath("$.totalUnrealisedProfitLossUsd").value(6000.00))  // 36000 - 30000
                .andExpect(jsonPath("$.totalRealisedProfitLossUsd").value(2500.00))  // 0.5 * (55000 - 50000) = 2500
                .andExpect(jsonPath("$.totalProfitLossUsd").value(8500.00))  // 6000 + 2500
                .andExpect(jsonPath("$.holdings").isArray())
                .andExpect(jsonPath("$.holdings").isNotEmpty());
    }
}
