package com.devita.domain.user.service;

import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.character.domain.Reward;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.PreferredCategory;
import com.devita.domain.user.domain.User;
import com.devita.domain.character.repository.RewardRepository;
import com.devita.domain.user.dto.PreferredCategoryRequest;
import com.devita.domain.user.dto.PreferredCategoryResponse;
import com.devita.domain.user.dto.UserInfoResponse;
import com.devita.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
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
                .profileImage("profile.jpg")
                .build();
        testUser.setId(USER_ID);

        testReward = Reward.builder()
                .user(testUser)
                .experience(0)
                .nutrition(0)
                .build();
    }

    @Test
    @DisplayName("선호 카테고리 업데이트 성공")
    void updatePreferredCategories_Success() {
        // given
        List<PreferredCategory> categories = Arrays.asList(
                PreferredCategory.JAVA,
                PreferredCategory.SPRING,
                PreferredCategory.REACT
        );
        PreferredCategoryRequest request = new PreferredCategoryRequest(categories);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        // when
        userService.updatePreferredCategories(USER_ID, request);

        // then
        assertEquals(categories, testUser.getPreferredCategories());
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("선호 카테고리 조회 성공")
    void getPreferredCategories_Success() {
        // given
        List<PreferredCategory> categories = Arrays.asList(
                PreferredCategory.JAVA,
                PreferredCategory.SPRING,
                PreferredCategory.REACT
        );
        testUser.updatePreferredCategories(categories);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        // when
        PreferredCategoryResponse response = userService.getPreferredCategories(USER_ID);

        // then
        assertEquals(categories, response.categories());
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("유저 정보 조회 성공")
    void getUserInfo_Success() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(rewardRepository.findById(USER_ID)).thenReturn(Optional.of(testReward));

        // when
        UserInfoResponse response = userService.getUserInfo(USER_ID);

        // then
        assertNotNull(response);
        assertEquals(testUser.getNickname(), response.nickname());
        assertEquals(testReward.getExperience(), response.experience());
        assertEquals(testReward.getNutrition(), response.nutrition());
        verify(userRepository).findById(USER_ID);
        verify(rewardRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 유저 조회 시 예외 발생")
    void getUserById_UserNotFound() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.getUserInfo(USER_ID));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 보상 정보가 없을 경우 예외 발생")
    void getUserInfo_RewardNotFound() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(rewardRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> userService.getUserInfo(USER_ID));

        // then
        assertEquals(ErrorCode.REWARD_NOT_FOUND, exception.getErrorCode());
    }
}
