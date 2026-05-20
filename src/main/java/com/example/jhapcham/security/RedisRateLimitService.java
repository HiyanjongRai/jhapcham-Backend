package com.example.jhapcham.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisRateLimitService {

    private final StringRedisTemplate redisTemplate;

    public void check(String bucket, String key, int maxRequests, Duration window) {
        if (key == null || key.isBlank() || maxRequests <= 0 || window == null || window.isZero()) {
            return;
        }

        long windowSeconds = window.toSeconds();
        long now = Instant.now().getEpochSecond();
        long windowStart = now - (now % windowSeconds);
        String redisKey = "rl:" + bucket + ":" + key + ":" + windowStart;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, window.plusSeconds(5));
        }
        if (count != null && count > maxRequests) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
        }
    }
}
