# Crypto Portfolio Backend - AI Agent Guide

## Architecture Overview
This is a Spring Boot 3.5.7 application using Java 21, providing REST APIs for crypto portfolio management. Key components:

- **Controllers**: REST endpoints in `controller/` package (Auth, MarketData, Portfolio, Transaction)
- **Services**: Business logic with interfaces/impl pattern in `service/` (e.g., `PortfolioServiceImpl`)
- **Data Layer**: JPA entities in `model/`, repositories in `repository/`, Flyway migrations in `src/main/resources/db/migration/`
- **Security**: JWT-based auth with Spring Security, user roles stored in `user_roles` table
- **External APIs**: CoinGecko integration via WebClient for market data
- **Scheduling**: Price update scheduler (currently commented out in `PriceUpdateScheduler.java`)

Data flows: User auth → JWT tokens → Secured endpoints → Service layer → Repositories → PostgreSQL DB. Market data fetched asynchronously via WebClient.

## Key Patterns & Conventions
- **Dependency Injection**: Use `@RequiredArgsConstructor` with `final` fields for services/repos (e.g., `PortfolioController`)
- **DTOs**: Request/response objects in `dto/` package, no entities exposed directly
- **BigDecimal Handling**: Use `setScale()` for money (2 decimals) and quantities (8 decimals), e.g., `money(holding.getQuantity().multiply(currentPriceUsd))`
- **User Context**: Access authenticated user via `SecurityContextHolder.getContext().getAuthentication()` in services
- **Exception Handling**: Custom exceptions in `exception/` package, global handler in config
- **Accounting Rules**: Transactions validated against available holdings (SELL cannot exceed holdings); portfolio summary computed from transaction history using weighted average cost
- **Testing**: Integration tests with `@SpringBootTest`, MockMvc for endpoints, `@MockBean` for external services like `MarketDataService`, H2 in-memory DB for isolation
- **Flyway Migrations**: Schema changes in `V1__init_schema.sql` and `V2__transaction_constraints.sql`, tables: users, coins, holdings, transactions, user_roles

## Developer Workflows
- **Run App**: `./mvnw spring-boot:run` (requires PostgreSQL running)
- **Database Setup**: `docker-compose up` for Postgres + PgAdmin (ports 5432, 5050)
- **Tests**: `./mvnw test` (includes integration tests with test profile using H2; requires no Docker)
- **Build**: `./mvnw clean package` (creates JAR in `target/`)
- **Debug**: Use Spring Boot DevTools, set breakpoints in service impls
- **API Docs**: Swagger UI at `/swagger-ui.html` (SpringDoc OpenAPI)

## Integration Points
- **CoinGecko API**: Configured in `WebClientConfig.java`, base URL in `application.yaml`
- **PostgreSQL**: Connection in `application.yaml`, schema managed by Flyway
- **JWT**: Secret and expiration in `application.yaml`, handled in `AuthServiceImpl`

Reference files: `pom.xml` for dependencies, `application.yaml` for config, `V1__init_schema.sql` for DB schema.
