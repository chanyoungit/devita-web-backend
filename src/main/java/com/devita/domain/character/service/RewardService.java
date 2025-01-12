package com.devita.domain.character.service;

import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.character.domain.Reward;
import com.devita.domain.character.enums.MissionEnum;
import com.devita.domain.character.domain.RewardType;
import com.devita.domain.character.repository.RewardRepository;
import com.devita.domain.todo.domain.Todo;
import com.devita.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {
    private final RedisTemplate<String, Integer> redisTemplate;
    private final RewardRepository rewardRepository;

    private static final int NUTRITION_THRESHOLD = 0;

    @Transactional
    public void processReward(User user, Todo todo) {
        MissionEnum missionEnum;

        try {
            missionEnum = MissionEnum.fromCategory(todo.getCategory().getName());
        } catch (IllegalArgumentException e) {
            log.error("잘못된 TodoEnum: {}", todo.getCategory().getName());
            throw new IllegalArgumentException(ErrorCode.INVALID_TODO_TYPE.getMessage());
        }

        String key = generateKey(user.getId(), missionEnum);

        if (!canReceiveReward(user.getId(), missionEnum)) {
            log.warn("{} 해당 유저의 {} 미션 완료 보상 지급 최대 한도 초과", user.getId(), missionEnum);
            throw new AccessDeniedException(ErrorCode.DAILY_REWARD_LIMIT_EXCEEDED);
        }

        // Redis 카운트 증가 또는 초기화
        try {
            Boolean keyExists = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(keyExists)) {
                redisTemplate.opsForValue().set(key, 1, getTimeUntilMidnight(), TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().increment(key);
            }
        } catch (Exception e) {
            log.error("Redis 서버와의 통신 중 오류 발생: {}", e.getMessage());
            throw new IllegalStateException(ErrorCode.REDIS_SERVER_ERROR.getMessage(), e);
        }

        // 보상 지급
        Reward reward = rewardRepository.findByUserId(user.getId())
                .orElseGet(() -> rewardRepository.save(
                        Reward.builder()
                                .user(user)
                                .experience(0)
                                .nutrition(0)
                                .build()
                ));

        RewardType rewardTypeInfo = missionEnum.getRewardType();
        switch (rewardTypeInfo.getType()) {
            case EXPERIENCE -> reward.addExperience(rewardTypeInfo.getAmount());
            case NUTRITION -> reward.addNutrition(rewardTypeInfo.getAmount());
        }

        rewardRepository.save(reward);
        log.info("유저 아이디 {}: 보상 타입={}, 수량={}",
                user.getId(), rewardTypeInfo.getType(), rewardTypeInfo.getAmount());
    }


    // 일일 보상 제한 확인
    private boolean canReceiveReward(Long userId, MissionEnum missionEnum) {
        String key = generateKey(userId, missionEnum);
        Integer count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            return true;
        }

        return count < missionEnum.getDailyLimit(); // MissionType에서 일일 제한을 가져옴

    }

    //레디스 키 생성
    private String generateKey(Long userId, MissionEnum todoEnum) {
        return userId + ":" + todoEnum.name();
    }

    // 레디스 TTL 설정
    private long getTimeUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    @Transactional
    public Long useNutrition(Long userId){
        Reward reward = rewardRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));

        if (reward.getNutrition() <= NUTRITION_THRESHOLD){
            throw new AccessDeniedException(ErrorCode.INSUFFICIENT_SUPPLEMENTS);
        }

        reward.useNutrition();

        return reward.getId();
    }
}