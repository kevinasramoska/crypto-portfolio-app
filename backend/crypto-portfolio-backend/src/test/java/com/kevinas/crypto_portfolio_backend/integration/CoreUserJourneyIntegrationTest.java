package com.kevinas.crypto_portfolio_backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreUserJourneyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        coinRepository.deleteAll();
        userRepository.deleteAll();

        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("65000.00"));
    }

    @Test
    void shouldCompleteCoreUserJourney_registerLoginBuySellAndGetPortfolioSummary() throws Exception {
        String email = "integration@test.com";
        String password = "Password123!";

        // 1. Register
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        // 2. Login
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.get("accessToken").asText();

        // 3. BUY 1 BTC @ 50000
        String buyBody = """
                {
                  "symbol": "BTC",
                  "name": "Bitcoin",
                  "type": "BUY",
                  "quantity": 1.00000000,
                  "priceUsd": 50000.00
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.type").value("BUY"));

        // 4. SELL 0.25 BTC @ 60000
        String sellBody = """
                {
                  "symbol": "BTC",
                  "name": "Bitcoin",
                  "type": "SELL",
                  "quantity": 0.25000000,
                  "priceUsd": 60000.00
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sellBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.type").value("SELL"));

        // 5. Verify portfolio summary
        mockMvc.perform(get("/api/portfolio/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestedUsd").value(37500.00))
                .andExpect(jsonPath("$.totalCurrentValueUsd").value(48750.00))
                .andExpect(jsonPath("$.totalUnrealisedProfitLossUsd").value(11250.00))
                .andExpect(jsonPath("$.totalRealisedProfitLossUsd").value(2500.00))
                .andExpect(jsonPath("$.totalProfitLossUsd").value(13750.00))
                .andExpect(jsonPath("$.holdings", hasSize(1)))
                .andExpect(jsonPath("$.holdings[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.holdings[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(0.75000000))
                .andExpect(jsonPath("$.holdings[0].averageBuyPriceUsd").value(50000.00))
                .andExpect(jsonPath("$.holdings[0].currentPriceUsd").value(65000.00))
                .andExpect(jsonPath("$.holdings[0].investedValueUsd").value(37500.00))
                .andExpect(jsonPath("$.holdings[0].currentValueUsd").value(48750.00))
                .andExpect(jsonPath("$.holdings[0].unrealisedProfitLossUsd").value(11250.00));
    }

    @Test
    void shouldRejectPortfolioSummaryRequest_withoutJwtToken() throws Exception {
        mockMvc.perform(get("/api/portfolio/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCreateTransaction_withoutJwtToken() throws Exception {
        String buyBody = """
            {
              "symbol": "BTC",
              "name": "Bitcoin",
              "type": "BUY",
              "quantity": 1.00000000,
              "priceUsd": 50000.00
            }
            """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyBody))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldRejectSellTransaction_whenUserTriesToSellMoreThanTheyOwn() throws Exception {
        String email = "oversell@test.com";
        String password = "Password123!";

        // Register
        String registerBody = """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        // Login
        String loginBody = """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String token = loginJson.get("accessToken").asText();

        // BUY 0.5 BTC @ 45000
        String buyBody = """
            {
              "symbol": "BTC",
              "name": "Bitcoin",
              "type": "BUY",
              "quantity": 0.50000000,
              "priceUsd": 45000.00
            }ƒ
            """;

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyBody))
                .andExpect(status().isOk());

        // Attempt to SELL 1.0 BTC (more than owned)
        String oversellBody = """
            {
              "symbol": "BTC",
              "name": "Bitcoin",
              "type": "SELL",
              "quantity": 1.00000000,
              "priceUsd": 60000.00
            }
            """;

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversellBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Holdings should remain unchanged
        mockMvc.perform(get("/api/portfolio/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestedUsd").value(22500.00))
                .andExpect(jsonPath("$.holdings", hasSize(1)))
                .andExpect(jsonPath("$.holdings[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(0.50000000))
                .andExpect(jsonPath("$.holdings[0].averageBuyPriceUsd").value(45000.00));
    }
}