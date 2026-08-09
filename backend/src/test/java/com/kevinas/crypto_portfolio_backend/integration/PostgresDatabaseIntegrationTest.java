package com.kevinas.crypto_portfolio_backend.integration;

import com.kevinas.crypto_portfolio_backend.config.TestConfig;
import com.kevinas.crypto_portfolio_backend.dto.TransactionRequest;
import com.kevinas.crypto_portfolio_backend.exception.InsufficientHoldingsException;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.Role;
import com.kevinas.crypto_portfolio_backend.model.Transaction;
import com.kevinas.crypto_portfolio_backend.model.TransactionType;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.TransactionRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.show-sql=false",
        "spring.task.scheduling.enabled=false",
        "portfolio.snapshots.scheduling.initial-delay-ms=86400000"
})
@Import(TestConfig.class)
class PostgresDatabaseIntegrationTest extends IntegrationTestSupport {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void flywayMigrations_shouldCreateTheExpectedSchemaAndConstraints() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select to_regclass('public.portfolio_snapshots')", String.class))
                .isEqualTo("portfolio_snapshots");

        jdbcTemplate.update("insert into users (email, password) values (?, ?)", "unique@example.com", "password-hash");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into users (email, password) values (?, ?)",
                "unique@example.com",
                "password-hash"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidSell_shouldRollbackCoinAndTransactionWrites() {
        User user = createAuthenticatedUser("rollback@example.com");

        assertThatThrownBy(() -> transactionService.createTransaction(new TransactionRequest(
                "ROLLBACK",
                "Rollback Coin",
                TransactionType.SELL,
                new BigDecimal("0.10000000"),
                new BigDecimal("60000.00")
        ))).isInstanceOf(InsufficientHoldingsException.class);

        assertThat(coinRepository.findBySymbolIgnoreCase("ROLLBACK")).isEmpty();
        assertThat(transactionRepository.findByUserOrderByCreatedAtDesc(user)).isEmpty();
        assertThat(holdingRepository.findByUser(user)).isEmpty();
    }

    @Test
    void buy_shouldPersistQuantitiesAndMoneyAtDatabaseScale() {
        User user = createAuthenticatedUser("decimal@example.com");
        when(marketDataService.getCurrentPrice("DECIMAL")).thenReturn(new BigDecimal("130.00"));

        transactionService.createTransaction(new TransactionRequest(
                "DECIMAL",
                "Decimal Coin",
                TransactionType.BUY,
                new BigDecimal("0.123456789"),
                new BigDecimal("123.456")
        ));

        Holding holding = holdingRepository.findByUserAndCoin_SymbolIgnoreCase(user, "DECIMAL").orElseThrow();
        Transaction transaction = transactionRepository.findByUserOrderByCreatedAtDesc(user).getFirst();

        assertThat(holding.getQuantity()).isEqualByComparingTo("0.12345679");
        assertThat(holding.getAverageBuyPriceUsd()).isEqualByComparingTo("123.46");
        assertThat(transaction.getQuantity()).isEqualByComparingTo("0.12345679");
        assertThat(transaction.getPriceUsd()).isEqualByComparingTo("123.46");
        assertThat(transaction.getTotalValueUsd()).isEqualByComparingTo("15.24");
    }

    private User createAuthenticatedUser(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password("password-hash")
                .roles(Set.of(Role.USER))
                .createdAt(Instant.now())
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
        );
        return user;
    }
}
