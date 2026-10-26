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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

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

        assertThat(migrationCount).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("select to_regclass('public.portfolio_snapshots')", String.class))
                .isEqualTo("portfolio_snapshots");
        assertThat(jdbcTemplate.queryForObject(
                "select to_regclass('public.uq_transactions_user_active_ledger_sequence')",
                String.class
        )).isEqualTo("uq_transactions_user_active_ledger_sequence");

        jdbcTemplate.update("insert into users (email, password) values (?, ?)", "unique@example.com", "password-hash");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into users (email, password) values (?, ?)",
                "unique@example.com",
                "password-hash"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transactionRevisionConstraints_shouldProtectActiveLedgerAndHoldingsUniqueness() {
        Long userId = jdbcTemplate.queryForObject(
                "insert into users (email, password) values ('revision-constraints@example.com', 'hash') returning id",
                Long.class
        );
        Long coinId = jdbcTemplate.queryForObject(
                "insert into coins (symbol, name) values ('REVISION', 'Revision Coin') returning id",
                Long.class
        );

        jdbcTemplate.update(
                "insert into transactions " +
                        "(created_at, price_usd, quantity, realised_profit_usd, total_value_usd, type, coin_id, user_id, ledger_sequence) " +
                        "values (now(), 10.00, 1.00000000, 0.00, 10.00, 'BUY', ?, ?, 1)",
                coinId,
                userId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into transactions " +
                        "(created_at, price_usd, quantity, realised_profit_usd, total_value_usd, type, coin_id, user_id, ledger_sequence) " +
                        "values (now(), 10.00, 1.00000000, 0.00, 10.00, 'BUY', ?, ?, 1)",
                coinId,
                userId
        )).isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "insert into holdings (quantity, average_buy_price_usd, coin_id, user_id) values (1.00000000, 10.00, ?, ?)",
                coinId,
                userId
        );
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into holdings (quantity, average_buy_price_usd, coin_id, user_id) values (2.00000000, 10.00, ?, ?)",
                coinId,
                userId
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
        assertThat(transactionRepository.findAllRevisionsByUser(user)).isEmpty();
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
                new BigDecimal("0.12345679"),
                new BigDecimal("123.456")
        ));

        Holding holding = holdingRepository.findByUserAndCoin_SymbolIgnoreCase(user, "DECIMAL").orElseThrow();
        Transaction transaction = transactionRepository.findAllRevisionsByUser(user).getFirst();

        assertThat(holding.getQuantity()).isEqualByComparingTo("0.12345679");
        assertThat(holding.getAverageBuyPriceUsd()).isEqualByComparingTo("123.456");
        assertThat(transaction.getQuantity()).isEqualByComparingTo("0.12345679");
        assertThat(transaction.getPriceUsd()).isEqualByComparingTo("123.456");
        assertThat(transaction.getTotalValueUsd()).isEqualByComparingTo("15.24");
    }

    @Test
    void backupRestoresIntoAnIsolatedDatabase(org.junit.jupiter.api.TestReporter reporter) throws Exception {
        User user = createAuthenticatedUser("restore-evidence@example.com");
        transactionService.createTransaction(new TransactionRequest("RESTORE", "Restore", TransactionType.BUY, BigDecimal.ONE, new BigDecimal("0.0049")));
        Integer originalCount = jdbcTemplate.queryForObject("SELECT count(*) FROM transactions", Integer.class);
        long started = System.nanoTime();
        assertThat(postgres.execInContainer("pg_dump", "--username", postgres.getUsername(), "--dbname", postgres.getDatabaseName(),
                "--format=custom", "--file=/tmp/portfolio-rehearsal.dump").getExitCode()).isZero();
        assertThat(postgres.execInContainer("createdb", "--username", postgres.getUsername(), "restore_rehearsal").getExitCode()).isZero();
        assertThat(postgres.execInContainer("pg_restore", "--username", postgres.getUsername(), "--exit-on-error", "--no-owner",
                "--dbname=restore_rehearsal", "/tmp/portfolio-rehearsal.dump").getExitCode()).isZero();
        try (var connection = java.sql.DriverManager.getConnection("jdbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(5432) + "/restore_rehearsal", postgres.getUsername(), postgres.getPassword());
             var statement = connection.prepareStatement("SELECT count(*) FROM transactions")) {
            var rows = statement.executeQuery();
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(originalCount);
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        reporter.publishEntry("synthetic-backup-restore-milliseconds", Long.toString(millis));
        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "Synthetic backup/restore: {0} ms", millis);
    }

    @Test
    void migratesLegacyRoundedHoldingsWithoutLosingAuditRows() throws Exception {
        var legacy = org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("legacy_precision").defaultSchema("legacy_precision").target("4").load();
        legacy.migrate();
        try (var connection = java.sql.DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var sql = connection.createStatement()) {
            connection.setSchema("legacy_precision");
            sql.executeUpdate("INSERT INTO users (id,email,password) VALUES (901,'legacy@example.com','hash')");
            sql.executeUpdate("INSERT INTO coins (id,symbol,name) VALUES (901,'LEGACY','Legacy')");
            sql.executeUpdate("INSERT INTO transactions (user_id,coin_id,type,quantity,price_usd,total_value_usd,realised_profit_usd,ledger_sequence,created_at) "
                    + "VALUES (901,901,'BUY',1,0.01,0.01,0,1,CURRENT_TIMESTAMP),(901,901,'BUY',1,0.02,0.02,0,2,CURRENT_TIMESTAMP)");
            sql.executeUpdate("INSERT INTO holdings (user_id,coin_id,quantity,average_buy_price_usd) VALUES (901,901,2,0.02)");
            sql.executeUpdate("INSERT INTO coins (id,symbol,name) VALUES (902,'LARGE','Large legacy values')");
            sql.executeUpdate("INSERT INTO transactions (user_id,coin_id,type,quantity,price_usd,total_value_usd,realised_profit_usd,ledger_sequence,created_at) "
                    + "SELECT 901,902,'BUY',1,10000000000000000,10000000000000000,0,n+2,CURRENT_TIMESTAMP FROM generate_series(1,100) AS n");
            sql.executeUpdate("INSERT INTO holdings (user_id,coin_id,quantity,average_buy_price_usd) VALUES (901,902,100,10000000000000000)");
        }
        org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("legacy_precision").defaultSchema("legacy_precision").load().migrate();
        assertThat(jdbcTemplate.queryForObject("SELECT cost_basis_usd FROM legacy_precision.holdings WHERE coin_id=901", BigDecimal.class)).isEqualByComparingTo("0.03");
        assertThat(jdbcTemplate.queryForObject("SELECT average_buy_price_usd FROM legacy_precision.holdings WHERE coin_id=901", BigDecimal.class)).isEqualByComparingTo("0.015");
        assertThat(jdbcTemplate.queryForObject("SELECT average_buy_price_usd FROM legacy_precision.holdings WHERE coin_id=902", BigDecimal.class)).isEqualByComparingTo("10000000000000000");
        assertThat(jdbcTemplate.queryForObject("SELECT cost_basis_usd FROM legacy_precision.holdings WHERE coin_id=902", BigDecimal.class)).isEqualByComparingTo("1000000000000000000");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM legacy_precision.transactions", Integer.class)).isEqualTo(102);
    }

    @Test
    void fullHistoryExportAllowsItsBoundaryAndRejectsTruncation() {
        User user = createAuthenticatedUser("bounded-export@example.com");
        var coin = coinRepository.save(com.kevinas.crypto_portfolio_backend.model.Coin.builder().symbol("EXPORT").name("Export").build());
        jdbcTemplate.update("INSERT INTO transactions (user_id,coin_id,type,quantity,price_usd,total_value_usd,realised_profit_usd,ledger_sequence,created_at) "
                + "SELECT ?,?,'BUY',1,1,1,0,n,CURRENT_TIMESTAMP FROM generate_series(1,10000) AS n", user.getId(), coin.getId());
        var exported = transactionService.getTransactionsForCurrentUser();
        assertThat(exported).hasSize(10000);
        assertThat(exported.getFirst().id()).isGreaterThan(exported.getLast().id());
        jdbcTemplate.update("INSERT INTO transactions (user_id,coin_id,type,quantity,price_usd,total_value_usd,realised_profit_usd,ledger_sequence,created_at) "
                + "VALUES (?,?,'BUY',1,1,1,0,10001,CURRENT_TIMESTAMP)", user.getId(), coin.getId());
        assertThatThrownBy(() -> transactionService.getTransactionsForCurrentUser())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("10,000");
    }

    @Test
    void simultaneousFirstUseOfACoinAcrossUsersDoesNotConflict() throws Exception {
        User first = createAuthenticatedUser("catalog-a@example.com");
        User second = createAuthenticatedUser("catalog-b@example.com");
        SecurityContextHolder.clearContext();
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.Stream.of(first, second).map(user -> executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.List.of()));
                try {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    return transactionService.createTransaction(new TransactionRequest("SHAREDNEW", user.getEmail(), TransactionType.BUY, BigDecimal.ONE, BigDecimal.ONE));
                } finally { SecurityContextHolder.clearContext(); }
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var task : tasks) assertThat(task.get(30, TimeUnit.SECONDS).symbol()).isEqualTo("SHAREDNEW");
        }
        assertThat(holdingRepository.findByUserAndCoin_SymbolIgnoreCase(first, "SHAREDNEW")).isPresent();
        assertThat(holdingRepository.findByUserAndCoin_SymbolIgnoreCase(second, "SHAREDNEW")).isPresent();
    }

    @Test
    void replaysAThousandActiveRowsWithPreciseBasis(org.junit.jupiter.api.TestReporter reporter) {
        User user = createAuthenticatedUser("history-scale@example.com");
        var coin = coinRepository.save(com.kevinas.crypto_portfolio_backend.model.Coin.builder().symbol("SCALE").name("Scale").build());
        jdbcTemplate.update("INSERT INTO transactions (user_id,coin_id,type,quantity,price_usd,total_value_usd,realised_profit_usd,ledger_sequence,created_at) "
                + "SELECT ?,?,'BUY',1,0.01,0.01,0,n,CURRENT_TIMESTAMP FROM generate_series(1,1000) AS n", user.getId(), coin.getId());
        long started = System.nanoTime();
        transactionService.createTransaction(new TransactionRequest("SCALE", "Scale", TransactionType.BUY, BigDecimal.ONE, new BigDecimal("0.02")));
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        reporter.publishEntry("1000-row-replay-milliseconds", Long.toString(millis));
        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "1,000-row replay plus committed snapshot: {0} ms", millis);
        var holding = holdingRepository.findByUserAndCoin_SymbolIgnoreCase(user, "SCALE").orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("1001");
        assertThat(holding.getCostBasisUsd()).isEqualByComparingTo("10.02");
        assertThat(transactionRepository.aggregateActiveTotals(user).totalBuyVolumeUsd()).isEqualByComparingTo("10.02");
    }

    @Test
    void concurrentCreatesForSameUser_shouldAllocateDistinctLedgerSequencesAndPreserveHolding() throws Exception {
        User user = createAuthenticatedUser("concurrent-ledger@example.com");
        SecurityContextHolder.clearContext();
        when(marketDataService.getCurrentPrice("LOCKED")).thenReturn(new BigDecimal("100.00"));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 2)
                    .mapToObj(index -> executor.submit(() -> {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user.getEmail(), null)
                        );
                        ready.countDown();
                        try {
                            start.await(10, TimeUnit.SECONDS);
                            return transactionService.createTransaction(new TransactionRequest(
                                    "LOCKED",
                                    "Locked Coin",
                                    TransactionType.BUY,
                                    new BigDecimal("1.00000000"),
                                    new BigDecimal("100.00")
                            ));
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }))
                    .toList();

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var task : tasks) {
                task.get(30, TimeUnit.SECONDS);
            }
        }

        assertThat(transactionRepository.findByUserAndVoidedAtIsNullOrderByLedgerSequenceAsc(user))
                .extracting(Transaction::getLedgerSequence)
                .containsExactly(1L, 2L);
        assertThat(holdingRepository.findByUserAndCoin_SymbolIgnoreCase(user, "LOCKED").orElseThrow().getQuantity())
                .isEqualByComparingTo("2.00000000");
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
