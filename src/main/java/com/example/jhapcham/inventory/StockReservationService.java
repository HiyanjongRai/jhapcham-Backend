package com.example.jhapcham.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class StockReservationService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            local qty = tonumber(ARGV[1])
            if stock < qty then
              return -1
            end
            redis.call('DECRBY', KEYS[1], qty)
            redis.call('SET', KEYS[2], qty, 'EX', ARGV[2], 'NX')
            return stock - qty
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean reserveProductStock(Long productId, int quantity, String idempotencyKey, Duration ttl) {
        String stockKey = "stock:product:" + productId;
        String reservationKey = "stock:reservation:" + idempotencyKey;
        Long remaining = redisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(stockKey, reservationKey),
                String.valueOf(quantity),
                String.valueOf(ttl.toSeconds()));
        return remaining != null && remaining >= 0;
    }

    public void releaseProductStock(Long productId, String idempotencyKey) {
        String reservationKey = "stock:reservation:" + idempotencyKey;
        String reserved = redisTemplate.opsForValue().get(reservationKey);
        if (reserved == null) {
            return;
        }
        redisTemplate.opsForValue().increment("stock:product:" + productId, Long.parseLong(reserved));
        redisTemplate.delete(reservationKey);
    }

    public void publishAvailableProductStock(Long productId, int stockQuantity) {
        redisTemplate.opsForValue().set("stock:product:" + productId, String.valueOf(Math.max(stockQuantity, 0)));
    }
}
