package com.kevinas.crypto_portfolio_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinas.crypto_portfolio_backend.config.TestConfig;
import com.kevinas.crypto_portfolio_backend.dto.*;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.PortfolioSnapshotRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
class TransactionControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

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
    void failedSell_shouldRollbackTransactionHoldingCoinAndSnapshotWrites() throws Exception {
        String email = "rollbacktransaction@example.com";
        String token = getJwtToken(email, "password");

        TransactionRequest sellRequest = new TransactionRequest(
                "ROLLBACK",
                "Rollback Coin",
                TransactionType.SELL,
                new BigDecimal("1.00000000"),
                new BigDecimal("60000.00")
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isConflict());

        var user = userRepository.findByEmail(email).orElseThrow();
        assertThat(coinRepository.findBySymbolIgnoreCase("ROLLBACK")).isEmpty();
        assertThat(holdingRepository.findByUser(user)).isEmpty();
        assertThat(transactionRepository.findAllRevisionsByUser(user)).isEmpty();
        assertThat(portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, Instant.EPOCH))
                .isEmpty();
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

    @Test
    void editingHistoricalBuy_shouldCreateReplacementReplayLedgerAndInvalidateSnapshots() throws Exception {
        String email = "editor@example.com";
        String token = getJwtToken(email, "password");
        when(marketDataService.getCurrentPrice("EDIT")).thenReturn(new BigDecimal("400.00"));

        TransactionResponse firstBuy = createTransaction(token, new TransactionRequest(
                "EDIT", "Edit Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("100.00")
        ));
        createTransaction(token, new TransactionRequest(
                "EDIT", "Edit Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("300.00")
        ));
        createTransaction(token, new TransactionRequest(
                "EDIT", "Edit Coin", TransactionType.SELL, new BigDecimal("1.00000000"), new BigDecimal("400.00")
        ));

        var user = userRepository.findByEmail(email).orElseThrow();
        Set<Long> oldSnapshotIds = portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, Instant.EPOCH)
                .stream()
                .map(snapshot -> snapshot.getId())
                .collect(java.util.stream.Collectors.toSet());

        TransactionRequest correction = new TransactionRequest(
                "EDIT", "Edit Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("200.00")
        );
        String responseBody = mockMvc.perform(put("/api/transactions/{id}", firstBuy.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(firstBuy.id().intValue())))
                .andReturn().getResponse().getContentAsString();

        TransactionResponse replacement = readTransactionResponse(responseBody);
        assertThat(replacement.createdAt()).isEqualTo(firstBuy.createdAt());

        List<com.kevinas.crypto_portfolio_backend.model.Transaction> allRevisions = transactionRepository
                .findAllRevisionsByUser(user);
        assertThat(allRevisions).hasSize(4);

        var original = transactionRepository.findById(firstBuy.id()).orElseThrow();
        assertThat(original.getVoidedAt()).isNotNull();
        assertThat(original.getReplacementTransaction().getId()).isEqualTo(replacement.id());
        assertThat(original.getLedgerSequence()).isEqualTo(1L);

        List<com.kevinas.crypto_portfolio_backend.model.Transaction> activeTransactions = transactionRepository
                .findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(user);
        assertThat(activeTransactions).hasSize(3);
        assertThat(activeTransactions).extracting(com.kevinas.crypto_portfolio_backend.model.Transaction::getLedgerSequence)
                .containsExactly(1L, 2L, 3L);
        assertThat(activeTransactions.getLast().getRealisedProfitUsd()).isEqualByComparingTo("150.00");

        var holding = holdingRepository.findByUserAndCoin_SymbolIgnoreCase(user, "EDIT").orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("1.00000000");
        assertThat(holding.getAverageBuyPriceUsd()).isEqualByComparingTo("250.00");

        var snapshotsAfterEdit = portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, Instant.EPOCH);
        assertThat(snapshotsAfterEdit).hasSize(1);
        assertThat(oldSnapshotIds).doesNotContain(snapshotsAfterEdit.getFirst().getId());
    }

    @Test
    void invalidHistoricalEdit_shouldRollbackAuditHoldingsAndSnapshots() throws Exception {
        String email = "invalid-editor@example.com";
        String token = getJwtToken(email, "password");
        when(marketDataService.getCurrentPrice("INVALIDEDIT")).thenReturn(new BigDecimal("200.00"));

        TransactionResponse buy = createTransaction(token, new TransactionRequest(
                "INVALIDEDIT", "Invalid Edit Coin", TransactionType.BUY,
                new BigDecimal("1.00000000"), new BigDecimal("100.00")
        ));
        createTransaction(token, new TransactionRequest(
                "INVALIDEDIT", "Invalid Edit Coin", TransactionType.SELL,
                new BigDecimal("1.00000000"), new BigDecimal("200.00")
        ));

        var user = userRepository.findByEmail(email).orElseThrow();
        List<Long> snapshotIdsBefore = portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, Instant.EPOCH)
                .stream()
                .map(snapshot -> snapshot.getId())
                .toList();

        TransactionRequest invalidCorrection = new TransactionRequest(
                "INVALIDEDIT", "Invalid Edit Coin", TransactionType.BUY,
                new BigDecimal("0.50000000"), new BigDecimal("100.00")
        );
        mockMvc.perform(put("/api/transactions/{id}", buy.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCorrection)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Transaction change would make a historical sell exceed available holdings"
                ));

        var unchangedBuy = transactionRepository.findById(buy.id()).orElseThrow();
        assertThat(unchangedBuy.getVoidedAt()).isNull();
        assertThat(unchangedBuy.getReplacementTransaction()).isNull();
        assertThat(transactionRepository.findAllRevisionsByUser(user)).hasSize(2);
        assertThat(holdingRepository.findByUser(user)).isEmpty();
        assertThat(portfolioSnapshotRepository
                .findByUserAndSnapshotAtGreaterThanEqualOrderBySnapshotAtAsc(user, Instant.EPOCH))
                .extracting(snapshot -> snapshot.getId())
                .containsExactlyElementsOf(snapshotIdsBefore);
    }

    @Test
    void deletingHistoricalBuy_shouldReplayLaterSellAndRejectStaleRevision() throws Exception {
        String email = "deleter@example.com";
        String token = getJwtToken(email, "password");
        when(marketDataService.getCurrentPrice("DELETE")).thenReturn(new BigDecimal("300.00"));

        TransactionResponse firstBuy = createTransaction(token, new TransactionRequest(
                "DELETE", "Delete Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("100.00")
        ));
        createTransaction(token, new TransactionRequest(
                "DELETE", "Delete Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("200.00")
        ));
        createTransaction(token, new TransactionRequest(
                "DELETE", "Delete Coin", TransactionType.SELL, new BigDecimal("1.00000000"), new BigDecimal("300.00")
        ));

        mockMvc.perform(delete("/api/transactions/{id}", firstBuy.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        var user = userRepository.findByEmail(email).orElseThrow();
        var activeTransactions = transactionRepository.findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(user);
        assertThat(activeTransactions).hasSize(2);
        assertThat(activeTransactions.getLast().getRealisedProfitUsd()).isEqualByComparingTo("100.00");
        assertThat(holdingRepository.findByUser(user)).isEmpty();

        mockMvc.perform(delete("/api/transactions/{id}", firstBuy.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void repeatedEdits_shouldFormAReplacementChainAndRejectAnOldRevisionId() throws Exception {
        String email = "replacement-chain@example.com";
        String token = getJwtToken(email, "password");
        when(marketDataService.getCurrentPrice("CHAIN")).thenReturn(new BigDecimal("100.00"));

        TransactionResponse original = createTransaction(token, new TransactionRequest(
                "CHAIN", "Chain Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("80.00")
        ));
        TransactionResponse secondRevision = updateTransaction(token, original.id(), new TransactionRequest(
                "CHAIN", "Chain Coin", TransactionType.BUY, new BigDecimal("1.50000000"), new BigDecimal("90.00")
        ));
        TransactionResponse thirdRevision = updateTransaction(token, secondRevision.id(), new TransactionRequest(
                "CHAIN", "Chain Coin", TransactionType.BUY, new BigDecimal("2.00000000"), new BigDecimal("100.00")
        ));

        mockMvc.perform(put("/api/transactions/{id}", original.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionRequest(
                                "CHAIN", "Chain Coin", TransactionType.BUY,
                                new BigDecimal("3.00000000"), new BigDecimal("100.00")
                        ))))
                .andExpect(status().isConflict());

        var user = userRepository.findByEmail(email).orElseThrow();
        var revisions = transactionRepository.findAllRevisionsByUser(user);
        assertThat(revisions).hasSize(3);
        assertThat(revisions).extracting(com.kevinas.crypto_portfolio_backend.model.Transaction::getLedgerSequence)
                .containsOnly(1L);
        assertThat(transactionRepository.findById(original.id()).orElseThrow().getReplacementTransaction().getId())
                .isEqualTo(secondRevision.id());
        assertThat(transactionRepository.findById(secondRevision.id()).orElseThrow().getReplacementTransaction().getId())
                .isEqualTo(thirdRevision.id());
        assertThat(transactionRepository.findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(user))
                .extracting(com.kevinas.crypto_portfolio_backend.model.Transaction::getId)
                .containsExactly(thirdRevision.id());
        assertThat(thirdRevision.createdAt()).isEqualTo(original.createdAt());
    }

    @Test
    void userCannotEditOrDeleteAnotherUsersTransaction() throws Exception {
        String ownerToken = getJwtToken("mutation-owner@example.com", "password");
        String otherToken = getJwtToken("mutation-other@example.com", "password");
        when(marketDataService.getCurrentPrice("OWNED")).thenReturn(new BigDecimal("100.00"));

        TransactionResponse owned = createTransaction(ownerToken, new TransactionRequest(
                "OWNED", "Owned Coin", TransactionType.BUY, new BigDecimal("1.00000000"), new BigDecimal("100.00")
        ));
        TransactionRequest correction = new TransactionRequest(
                "OWNED", "Owned Coin", TransactionType.BUY, new BigDecimal("2.00000000"), new BigDecimal("100.00")
        );

        mockMvc.perform(put("/api/transactions/{id}", owned.id())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correction)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/transactions/{id}", owned.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        var owner = userRepository.findByEmail("mutation-owner@example.com").orElseThrow();
        assertThat(transactionRepository.findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(owner)).hasSize(1);
        assertThat(holdingRepository.findByUserAndCoin_SymbolIgnoreCase(owner, "OWNED").orElseThrow().getQuantity())
                .isEqualByComparingTo("1.00000000");
    }

    private TransactionResponse createTransaction(String token, TransactionRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return readTransactionResponse(response);
    }

    private TransactionResponse updateTransaction(String token, Long id, TransactionRequest request) throws Exception {
        String response = mockMvc.perform(put("/api/transactions/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return readTransactionResponse(response);
    }

    private TransactionResponse readTransactionResponse(String response) throws Exception {
        var json = objectMapper.readTree(response);
        return new TransactionResponse(
                json.get("id").longValue(),
                json.get("symbol").textValue(),
                json.get("name").textValue(),
                TransactionType.valueOf(json.get("type").textValue()),
                json.get("quantity").decimalValue(),
                json.get("priceUsd").decimalValue(),
                json.get("totalValueUsd").decimalValue(),
                json.get("realisedProfitUsd").decimalValue(),
                Instant.parse(json.get("createdAt").textValue())
        );
    }
}
