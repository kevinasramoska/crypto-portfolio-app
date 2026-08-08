package com.kevinas.crypto_portfolio_backend.integration;

import com.kevinas.crypto_portfolio_backend.config.TestConfig;
import com.kevinas.crypto_portfolio_backend.dto.SupportedCoinResponse;
import com.kevinas.crypto_portfolio_backend.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
class MarketDataControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketDataService marketDataService;

    @Test
    void supportedCoins_shouldReturnBackendSupportedMappings() throws Exception {
        when(marketDataService.getSupportedCoins()).thenReturn(List.of(
                new SupportedCoinResponse("BTC", "Bitcoin", "bitcoin"),
                new SupportedCoinResponse("ETH", "Ethereum", "ethereum")
        ));

        mockMvc.perform(get("/api/market/supported-coins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].coinGeckoId").value("bitcoin"))
                .andExpect(jsonPath("$[1].symbol").value("ETH"));
    }
}
