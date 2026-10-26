package com.kevinas.crypto_portfolio_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.config.TestConfig;
import com.kevinas.crypto_portfolio_backend.dto.*;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
class AuditHardeningIntegrationTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void subCentPricesAndWeightedBasisSurviveReplay() throws Exception {
        String token = register("precision-" + UUID.randomUUID() + "@example.com");
        when(marketDataService.getCurrentPrice("TINY")).thenReturn(new BigDecimal("0.0049"));
        trade(token, "TINY", "Tiny", "100", "0.004", TransactionType.BUY);
        mvc.perform(get("/api/portfolio/summary").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCurrentValueUsd").value(0.49))
                .andExpect(jsonPath("$.totalInvestedUsd").value(0.40));
        trade(token, "BASIS", "Basis", "1", "0.01", TransactionType.BUY);
        trade(token, "BASIS", "Basis", "1", "0.02", TransactionType.BUY);
        when(marketDataService.getCurrentPrice("BASIS")).thenReturn(new BigDecimal("0.02"));
        mvc.perform(get("/api/portfolio/holdings").header("Authorization", token))
                .andExpect(jsonPath("$[?(@.symbol == 'BASIS')].averageBuyPriceUsd").value(org.hamcrest.Matchers.contains(0.015)))
                .andExpect(jsonPath("$[?(@.symbol == 'BASIS')].investedValueUsd").value(org.hamcrest.Matchers.contains(0.03)));
        trade(token, "BASIS", "Basis", "1", "0.02", TransactionType.SELL);
        mvc.perform(get("/api/portfolio/summary").header("Authorization", token))
                .andExpect(jsonPath("$.totalUnrealisedProfitLossUsd").value(0.10))
                .andExpect(jsonPath("$.totalRealisedProfitLossUsd").value(0.01))
                .andExpect(jsonPath("$.totalProfitLossUsd").value(0.10));
        trade(token, "BASIS", "Basis", "1", "0.02", TransactionType.SELL);
        mvc.perform(get("/api/transactions/summary").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRealisedProfitUsd").value(0.01));
    }

    @Test
    void roundsAggregatesOnlyAfterCombiningExactPositionAndTradeValues() throws Exception {
        String token = register("rounding-" + UUID.randomUUID() + "@example.com");
        for (String symbol : java.util.List.of("ROUNDONE", "ROUNDTWO")) {
            when(marketDataService.getCurrentPrice(symbol)).thenReturn(new BigDecimal("0.01"));
            trade(token, symbol, symbol, "1", "0.005", TransactionType.BUY);
        }
        mvc.perform(get("/api/portfolio/summary").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalInvestedUsd").value(0.01))
                .andExpect(jsonPath("$.totalCurrentValueUsd").value(0.02))
                .andExpect(jsonPath("$.totalUnrealisedProfitLossUsd").value(0.01))
                .andExpect(jsonPath("$.totalProfitLossUsd").value(0.01));
        mvc.perform(get("/api/transactions/summary").header("Authorization", token))
                .andExpect(jsonPath("$.totalBuyVolumeUsd").value(0.01));
    }

    @Test
    void customNamesAreOwnedByEachUsersLedger() throws Exception {
        String first = register("name-a-" + UUID.randomUUID() + "@example.com");
        String second = register("name-b-" + UUID.randomUUID() + "@example.com");
        trade(first, "CUSTOM", "=1+1", "1", "1", TransactionType.BUY);
        trade(second, "CUSTOM", "Clean asset", "1", "1", TransactionType.BUY);
        mvc.perform(get("/api/portfolio/holdings").header("Authorization", second))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Clean asset"));
    }

    @Test
    void staleAndMissingPricesDoNotProduceCertifiedSnapshots() throws Exception {
        String token = register("stale-" + UUID.randomUUID() + "@example.com");
        when(marketDataService.getCurrentPrice("BTC")).thenReturn(new BigDecimal("60000"));
        when(marketDataService.getPriceMetadata("BTC")).thenReturn(new MarketQuote(new BigDecimal("60000"), Instant.now().minusSeconds(90), "STALE"));
        trade(token, "BTC", "Bitcoin", "1", "50000", TransactionType.BUY);
        mvc.perform(get("/api/portfolio/summary").header("Authorization", token))
                .andExpect(jsonPath("$.hasStaleMarketData").value(true));
        mvc.perform(get("/api/portfolio/performance/history?range=7d").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.history[0].valuationComplete").value(false));
    }

    @Test
    void validatesPrecisionAndBoundsWithoutInvalidatingSession() throws Exception {
        String token = register("limits-" + UUID.randomUUID() + "@example.com");
        var invalid = new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("0.000000001"), BigDecimal.ONE);
        mvc.perform(post("/api/transactions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/transactions/paginated?size=2147483647").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/transactions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{bad json")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/portfolio/summary").header("Authorization", token)).andExpect(status().isOk());
    }

    @Test
    void normalizesIdentityAndRejectsOversizedUtf8Passwords() throws Exception {
        String email = "Case-" + UUID.randomUUID() + "@Example.COM";
        register(" " + email + " ");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(email.toLowerCase(java.util.Locale.ROOT), "Password123!"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RegisterRequest("utf8-" + UUID.randomUUID() + "@example.com", "é".repeat(40)))))
                .andExpect(status().isBadRequest());
    }

    private String register(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RegisterRequest(email, "Password123!"))))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + json.readValue(result.getResponse().getContentAsString(), JwtResponse.class).accessToken();
    }

    private void trade(String token, String symbol, String name, String quantity, String price, TransactionType type) throws Exception {
        mvc.perform(post("/api/transactions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new TransactionRequest(symbol, name, type, new BigDecimal(quantity), new BigDecimal(price)))))
                .andExpect(status().isOk());
    }
}
