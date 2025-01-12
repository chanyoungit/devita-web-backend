package com.devita.common.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

// 서버에서 관리하는 리프레시 토큰을 관리하기 위한 로직
@Service
@Slf4j
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 리프레시 토큰 저장 (TTL 설정)
    public void saveRefreshToken(Long userId, String refreshToken, long expirationTimeInMillis) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(String.valueOf(userId), refreshToken, expirationTimeInMillis, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(Long userId) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(String.valueOf(userId));
    }

    public void deleteRefreshToken(Long userId) {
        log.info(String.valueOf(userId));
        redisTemplate.delete(String.valueOf(userId));
    }

    public boolean hasValidRefreshToken(Long userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }
}
