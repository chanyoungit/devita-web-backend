package com.devita.domain.character.service;

import com.devita.common.constant.CategoryConstants;
import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.IllegalArgumentException;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.todo.domain.*;
import com.devita.domain.character.domain.Reward;
import com.devita.domain.character.repository.RewardRepository;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {
    @Mock
    private RedisTemplate<String, Integer> redisTemplate;
    @Mock
    private ValueOperations<String, Integer> valueOperations;
    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private RewardService rewardService;

    private User testUser;
    private Todo testTodo;
    private Category testCategory;
    private Reward testReward;
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";
    private static final String USER_NICKNAME = "testUser";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(USER_EMAIL)
                .nickname(USER_NICKNAME)
                .provider(AuthProvider.KAKAO)
                .build();
        testUser.setId(USER_ID);

        testCategory = new Category(testUser, CategoryConstants.DAILY_MISSION_CATEGORY, "#000000");

        testTodo = Todo.builder()
                .user(testUser)
                .category(testCategory)
                .title("Test Todo")
                .status(false)
                .date(LocalDate.now())
                .build();

        testReward = Reward.builder()
                .user(testUser)
                .experience(10)
                .nutrition(3)
                .build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("보상 처리 성공")
    void processReward_Success() {
        // given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(rewardRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testReward));

        // when
        rewardService.processReward(testUser, testTodo);

        // then
        verify(valueOperations).set(anyString(), eq(1), anyLong(), eq(TimeUnit.SECONDS));
        verify(rewardRepository).save(testReward);
        assertEquals(13, testReward.getExperience()); // DAILY_MISSION gives 3 experience
    }

    @Test
    @DisplayName("일일 보상 한도 초과 시 보상 처리 실패")
    void processReward_DailyLimitExceeded() {
        // given
        when(valueOperations.get(anyString())).thenReturn(1); // DAILY_MISSION has limit 1

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> rewardService.processReward(testUser, testTodo));

        // then
        assertEquals(ErrorCode.DAILY_REWARD_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("레디스 오류 발생 시 보상 처리 실패")
    void processReward_RedisError() {
        // given
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis Error"));

        // when
        RuntimeException exception = assertThrows(RuntimeException.class, () -> rewardService.processReward(testUser, testTodo));

        // then
        assertEquals("Redis Error", exception.getMessage());
    }

    @Test
    @DisplayName("영양제 사용 성공")
    void useNutrition_Success() {
        // given
        when(rewardRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testReward));

        // when
        Long rewardId = rewardService.useNutrition(USER_ID);

        // then
        assertEquals(testReward.getId(), rewardId);
        assertEquals(40, testReward.getExperience());
        assertEquals(2, testReward.getNutrition());
    }

    @Test
    @DisplayName("영양제 부족 시 영양제 사용 실패")
    void useNutrition_InsufficientNutrition() {
        // given
        testReward.useNutrition();
        testReward.useNutrition();
        testReward.useNutrition();
        when(rewardRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testReward));

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> rewardService.useNutrition(USER_ID));

        // then
        assertEquals(ErrorCode.INSUFFICIENT_SUPPLEMENTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("보상 정보 미발견 시 영양제 사용 실패")
    void useNutrition_RewardNotFound() {
        // given
        when(rewardRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> rewardService.useNutrition(USER_ID));

        // then
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }
}
