package com.kevinas.crypto_portfolio_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RequestRateLimiter requestRateLimiter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String group = rateLimitGroup(request);
        if (group == null) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestRateLimiter.RateLimitDecision decision = requestRateLimiter.tryAcquire(group, request.getRemoteAddr());
        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String rateLimitGroup(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod())
                && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path))) {
            return "auth";
        }
        if ("GET".equals(request.getMethod()) && path.startsWith("/api/market/")) {
            return "market";
        }
        return null;
    }
}
