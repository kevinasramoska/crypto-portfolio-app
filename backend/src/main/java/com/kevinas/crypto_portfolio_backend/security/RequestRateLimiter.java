package com.kevinas.crypto_portfolio_backend.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestRateLimiter {

    private final Map<String, ClientWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();
    private final Clock clock;

    @Value("${rate-limit.auth.max-requests}")
    private int authMaxRequests;

    @Value("${rate-limit.auth.window-seconds}")
    private long authWindowSeconds;

    @Value("${rate-limit.market.max-requests}")
    private int marketMaxRequests;

    @Value("${rate-limit.market.window-seconds}")
    private long marketWindowSeconds;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @PostConstruct
    void validateConfiguration() {
        validatePolicy("auth", authMaxRequests, authWindowSeconds);
        validatePolicy("market", marketMaxRequests, marketWindowSeconds);
    }

    public RateLimitDecision tryAcquire(String group, String clientAddress) {
        RateLimitPolicy policy = policyFor(group);
        long now = clock.millis();
        ClientWindow window = windows.computeIfAbsent(group + ":" + clientAddress, ignored -> new ClientWindow());
        RateLimitDecision decision = window.tryAcquire(now, policy);

        if (requestCount.incrementAndGet() % 256 == 0) {
            removeExpiredWindows(now);
        }

        return decision;
    }

    private RateLimitPolicy policyFor(String group) {
        return switch (group) {
            case "auth" -> new RateLimitPolicy(authMaxRequests, authWindowSeconds);
            case "market" -> new RateLimitPolicy(marketMaxRequests, marketWindowSeconds);
            default -> throw new IllegalArgumentException("Unsupported rate-limit group: " + group);
        };
    }

    private void removeExpiredWindows(long now) {
        long longestWindowMillis = Math.max(authWindowSeconds, marketWindowSeconds) * 1000;
        windows.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAtMillis() >= longestWindowMillis);
    }

    private void validatePolicy(String group, int maxRequests, long windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            throw new IllegalStateException("Rate-limit " + group + " settings must be positive");
        }
    }

    public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    }

    private record RateLimitPolicy(int maxRequests, long windowSeconds) {
        private long windowMillis() {
            return windowSeconds * 1000;
        }
    }

    private static final class ClientWindow {
        private final ArrayDeque<Long> requestTimes = new ArrayDeque<>();
        private long lastSeenAtMillis;

        synchronized RateLimitDecision tryAcquire(long now, RateLimitPolicy policy) {
            long windowStart = now - policy.windowMillis();
            while (!requestTimes.isEmpty() && requestTimes.peekFirst() <= windowStart) {
                requestTimes.removeFirst();
            }

            lastSeenAtMillis = now;
            if (requestTimes.size() >= policy.maxRequests()) {
                long retryAfterMillis = requestTimes.peekFirst() + policy.windowMillis() - now;
                return new RateLimitDecision(false, Math.max(1, (retryAfterMillis + 999) / 1000));
            }

            requestTimes.addLast(now);
            return new RateLimitDecision(true, 0);
        }

        synchronized long lastSeenAtMillis() {
            return lastSeenAtMillis;
        }
    }
}
