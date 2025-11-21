package com.kevinas.crypto_portfolio_backend.service;

import com.kevinas.crypto_portfolio_backend.dto.HoldingRequest;
import com.kevinas.crypto_portfolio_backend.dto.HoldingResponse;
import com.kevinas.crypto_portfolio_backend.model.Coin;
import com.kevinas.crypto_portfolio_backend.model.Holding;
import com.kevinas.crypto_portfolio_backend.model.User;
import com.kevinas.crypto_portfolio_backend.repository.CoinRepository;
import com.kevinas.crypto_portfolio_backend.repository.HoldingRepository;
import com.kevinas.crypto_portfolio_backend.repository.UserRepository;
import com.kevinas.crypto_portfolio_backend.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final HoldingRepository holdingRepository;
    private final CoinRepository coinRepository;
    private final UserRepository userRepository;

    @Override
    public List<HoldingResponse> getUserHoldings() {
        User user = getCurrentUser();
        return holdingRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public HoldingResponse addHolding(HoldingRequest request) {
        User user = getCurrentUser();
        Coin coin = coinRepository.findBySymbolIgnoreCase(request.symbol())
                .orElseGet(() -> coinRepository.save(
                        Coin.builder()
                                .symbol(request.symbol().toUpperCase())
                                .name(request.symbol().toUpperCase())
                                .build()
                ));

        Holding holding = Holding.builder()
                .user(user)
                .coin(coin)
                .quantity(request.quantity())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toResponse(holdingRepository.save(holding));
    }

    @Override
    public HoldingResponse updateHolding(Long id, HoldingRequest request) {
        User user = getCurrentUser();
        Holding holding = holdingRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        holding.setQuantity(request.quantity());
        holding.setUpdatedAt(Instant.now());
        return toResponse(holdingRepository.save(holding));
    }

    @Override
    public void deleteHolding(Long id) {
        User user = getCurrentUser();
        Holding holding = holdingRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        holdingRepository.delete(holding);
    }

    private HoldingResponse toResponse(Holding holding) {
        return new HoldingResponse(
                holding.getId(),
                holding.getCoin().getSymbol(),
                holding.getCoin().getName(),
                holding.getQuantity()
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Logged in user not found"));
    }
}