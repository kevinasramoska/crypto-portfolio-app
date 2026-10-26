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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
class TransactionHistoryIntegrationTest extends IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void backdatedBuysReplayInExecutionOrderRatherThanEntryOrder() throws Exception {
        String token = register();
        trade(token, "BTC", "BUY", "2", "20", "2026-01-02T12:00:00Z");
        var sell = trade(token, "BTC", "SELL", "1", "30", "2026-01-03T12:00:00Z");
        trade(token, "BTC", "BUY", "1", "10", "2026-01-01T12:00:00Z");
        mvc.perform(get("/api/transactions?type=SELL").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(sell.id()))
                .andExpect(jsonPath("$[0].realisedProfitUsd").value(13.33));
        mvc.perform(get("/api/portfolio/holdings").header("Authorization", token))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void rejectsHistoricalOversellsAndInvalidTimesWithoutChangingHistory() throws Exception {
        String token = register();
        trade(token, "BTC", "BUY", "1", "10", "2026-01-02T00:00:00Z");
        mvc.perform(post("/api/transactions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(payload("BTC", "SELL", "1", "20", "2026-01-01T00:00:00Z"))).andExpect(status().isConflict());
        for (String date : new String[]{"2999-01-01T00:00:00Z", "1969-01-01T00:00:00Z"}) {
            mvc.perform(post("/api/transactions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                    .content(payload("BTC", "BUY", "1", "20", date))).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/transactions").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void editingExecutionTimeRollsBackInvalidOrderingAndPreservesAuditTime() throws Exception {
        String token = register();
        var buy = trade(token, "BTC", "BUY", "2", "10", "2026-01-01T00:00:00Z");
        trade(token, "BTC", "SELL", "1", "20", "2026-01-02T00:00:00Z");
        mvc.perform(put("/api/transactions/" + buy.id()).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(payload("BTC", "BUY", "2", "10", "2026-01-03T00:00:00Z"))).andExpect(status().isConflict());
        var result = mvc.perform(put("/api/transactions/" + buy.id()).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new TransactionRequest("BTC", "Bitcoin", TransactionType.BUY, new BigDecimal("2"), new BigDecimal("11")))))
                .andExpect(status().isOk()).andReturn();
        var revision = readTrade(result.getResponse().getContentAsString());
        assertThat(revision.createdAt()).isEqualTo(buy.createdAt());
        assertThat(revision.executedAt()).isEqualTo(buy.executedAt());
        assertThat(revision.id()).isNotEqualTo(buy.id());
        mvc.perform(get("/api/transactions").header("Authorization", token)).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filtersAndExportsOnlyTheOwnersMatchingActiveTradesWithUtcBounds() throws Exception {
        String token = register();
        trade(token, "BTC", "BUY", "1", "10", "2026-01-01T00:00:00Z");
        trade(token, "BTC", "BUY", "2", "20", "2026-01-02T00:00:00Z");
        trade(token, "BTC", "BUY", "3", "30", "2026-01-03T00:00:00Z");
        trade(token, "ETH", "BUY", "1", "5", "2026-01-02T00:00:00Z");
        trade(register(), "BTC", "BUY", "10", "100", "2026-01-02T00:00:00Z");
        String query = "symbol=btc&type=BUY&from=2026-01-01T00:00:00Z&to=2026-01-03T00:00:00Z&sort=totalValueUsd&direction=asc";
        mvc.perform(get("/api/transactions/paginated?size=1&" + query).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].totalValueUsd").value(10)).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/transactions?" + query).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].totalValueUsd").value(40));
    }

    @Test
    void normalizesOffsetTimesAndRejectsInvalidFilterOrSortInput() throws Exception {
        String token = register();
        var trade = trade(token, "BTC", "BUY", "1", "10", "2026-01-01T02:00:00+02:00");
        assertThat(trade.executedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        for (String query : new String[]{"sort=user.password", "direction=random", "symbol=BTC%25", "type=OTHER",
                "from=invalid", "from=2026-02-01T00:00:00Z&to=2026-01-01T00:00:00Z", "size=101"}) {
            mvc.perform(get("/api/transactions/paginated?" + query).header("Authorization", token)).andExpect(status().isBadRequest());
        }
    }

    private String register() throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RegisterRequest("history-" + UUID.randomUUID() + "@example.com", "Password123!"))))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + json.readValue(result.getResponse().getContentAsString(), JwtResponse.class).accessToken();
    }

    private String payload(String symbol, String type, String quantity, String price, String time) {
        return "{\"symbol\":\"" + symbol + "\",\"name\":\"Sample asset\",\"type\":\"" + type
                + "\",\"quantity\":" + quantity + ",\"priceUsd\":" + price + ",\"executedAt\":\"" + time + "\"}";
    }

    private RecordedTrade trade(String token, String symbol, String type, String quantity, String price, String time) throws Exception {
        var response = mvc.perform(post("/api/transactions").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(payload(symbol, type, quantity, price, time))).andExpect(status().isOk()).andReturn();
        return readTrade(response.getResponse().getContentAsString());
    }

    private RecordedTrade readTrade(String response) throws Exception {
        var node = json.readTree(response);
        return new RecordedTrade(node.get("id").longValue(), Instant.parse(node.get("createdAt").textValue()),
                Instant.parse(node.get("executedAt").textValue()));
    }

    private record RecordedTrade(Long id, Instant createdAt, Instant executedAt) {}
}
