package com.fraudpipeline.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    IdempotencyService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new IdempotencyService(redis, 24);
    }

    @Test
    void returnsEmptyWhenKeyNotInRedis() {
        when(valueOps.get(anyString())).thenReturn(null);

        Optional<String> result = service.getCachedResponse("merchant-1", "order-1");

        assertThat(result).isEmpty();
    }

    @Test
    void returnsCachedResponseWhenKeyExists() {
        when(valueOps.get(anyString())).thenReturn("{\"status\":\"SETTLED\"}");

        Optional<String> result = service.getCachedResponse("merchant-1", "order-1");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("SETTLED");
    }

    @Test
    void differentMerchantsProduceDifferentKeys() {
        service.cacheResponse("merchant-1", "order-1", "response-A");
        service.cacheResponse("merchant-2", "order-1", "response-B");

        // Two separate Redis writes with different keys
        verify(valueOps, times(2)).set(anyString(), anyString(), any());
    }

    @Test
    void sameMerchantAndKeyCachesOnce() {
        service.cacheResponse("merchant-1", "order-1", "response");

        verify(valueOps, times(1)).set(anyString(), eq("response"), any());
    }
}
