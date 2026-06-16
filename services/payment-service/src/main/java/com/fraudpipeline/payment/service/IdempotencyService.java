package com.fraudpipeline.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redis;
    private final long ttlHours;

    public IdempotencyService(
            StringRedisTemplate redis,
            @Value("${idempotency.ttl-hours}") long ttlHours) {
        this.redis = redis;
        this.ttlHours = ttlHours;
    }

    public Optional<String> getCachedResponse(String merchantId, String idempotencyKey) {
        String redisKey = buildKey(merchantId, idempotencyKey);
        return Optional.ofNullable(redis.opsForValue().get(redisKey));
    }

    public void cacheResponse(String merchantId, String idempotencyKey, String responseJson) {
        String redisKey = buildKey(merchantId, idempotencyKey);
        redis.opsForValue().set(redisKey, responseJson, Duration.ofHours(ttlHours));
    }

    private String buildKey(String merchantId, String idempotencyKey) {
        try {
            String raw = merchantId + ":" + idempotencyKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return "idempotency:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
