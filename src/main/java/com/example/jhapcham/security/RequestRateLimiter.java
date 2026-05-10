package com.example.jhapcham.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simple in-memory fixed-window rate limiter.
 *
 * Notes:
 * - Intended for demos / single-node deployments.
 * - For multi-instance production, move this to a shared store (Redis, etc.).
 */
@Component
public class RequestRateLimiter {

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void check(String key, int maxRequests, long windowSeconds) {
        if (key == null || key.isBlank()) {
            // If we can't key it safely, don't accidentally DoS everyone under one bucket.
            return;
        }
        if (maxRequests <= 0 || windowSeconds <= 0) {
            return;
        }

        long nowEpoch = Instant.now(clock).getEpochSecond();
        long windowStart = nowEpoch - (nowEpoch % windowSeconds);

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStartEpochSeconds != windowStart) {
                return new Window(windowStart);
            }
            return existing;
        });

        int count = window.counter.incrementAndGet();
        if (count > maxRequests) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
        }
    }

    private static final class Window {
        final long windowStartEpochSeconds;
        final AtomicInteger counter = new AtomicInteger(0);

        Window(long windowStartEpochSeconds) {
            this.windowStartEpochSeconds = windowStartEpochSeconds;
        }
    }
}

