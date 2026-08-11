package com.kevinas.crypto_portfolio_backend.integration;

import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class IntegrationTestSupport {

    @MockitoBean
    protected MarketDataService marketDataService;
}
