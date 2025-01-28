package com.devita.domain.follow.service;

import com.devita.domain.follow.dto.FollowResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FollowCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long CACHE_TTL = 1;

    // 팔로잉 목록 캐시 저장
    public void cacheFollowings(Long userId, List<FollowResponseDTO> followings) {
        String key = "followings:" + userId;
        redisTemplate.opsForValue().set(key, followings, CACHE_TTL, TimeUnit.HOURS);
    }

    // 팔로잉 목록 캐시 불러오기
    public List<FollowResponseDTO> getFollowingsFromCache(Long userId) {
        String key = "followings:" + userId;
        return (List<FollowResponseDTO>) redisTemplate.opsForValue().get(key);
    }

    // 팔로워 목록 캐시 저장
    public void cacheFollowers(Long userId, List<FollowResponseDTO> followers) {
        String key = "followers:" + userId;
        redisTemplate.opsForValue().set(key, followers, CACHE_TTL, TimeUnit.HOURS);
    }

    // 팔로워 목록 캐시 불러오기
    public List<FollowResponseDTO> getFollowersFromCache(Long userId) {
        String key = "followers:" + userId;
        return (List<FollowResponseDTO>) redisTemplate.opsForValue().get(key);
    }

    // 팔로우 캐시 삭제
    public void deleteFollowCache(Long userId) {
        redisTemplate.delete("followers:" + userId);
        redisTemplate.delete("followings:" + userId);
    }
}
