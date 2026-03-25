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
class TransactionControllerIntegrationTest {

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
    void authenticatedBuyTransaction_shouldSucceed() throws Exception {
        String token = getJwtToken("buyer@example.com", "password");

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("50000.00"));

        TransactionRequest request = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("50000.00"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.quantity").value(1.0));
    }

    @Test
    void secondBuy_shouldUpdateHoldings() throws Exception {
        String token = getJwtToken("secondbuyer@example.com", "password");

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("50000.00"));

        // First buy
        TransactionRequest request1 = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("50000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Second buy
        TransactionRequest request2 = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("0.50000000"), new BigDecimal("60000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());
    }

    @Test
    void sellWithinAvailableQuantity_shouldSucceed() throws Exception {
        String token = getJwtToken("seller@example.com", "password");

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("50000.00"));

        // Buy first
        TransactionRequest buyRequest = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("50000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk());

        // Sell
        TransactionRequest sellRequest = new TransactionRequest("BTC", "Bitcoin", TransactionType.SELL, new BigDecimal("0.50000000"), new BigDecimal("60000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void sellAboveAvailableQuantity_shouldFail() throws Exception {
        String token = getJwtToken("overseller@example.com", "password");

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("50000.00"));

        // Buy 0.5
        TransactionRequest buyRequest = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("0.50000000"), new BigDecimal("50000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk());

        // Try to sell 1.0
        TransactionRequest sellRequest = new TransactionRequest("BTC", "Bitcoin", TransactionType.SELL, new BigDecimal("1.00000000"), new BigDecimal("60000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void userCannotAccessAnotherUsersData() throws Exception {
        String token1 = getJwtToken("user1@example.com", "password");
        String token2 = getJwtToken("user2@example.com", "password");

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("50000.00"));

        // User1 buys
        TransactionRequest request = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("50000.00"));
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // User2 tries to get transactions, should be empty
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
