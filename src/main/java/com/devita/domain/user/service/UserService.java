package com.devita.domain.user.service;

import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.character.domain.Reward;
import com.devita.domain.character.repository.RewardRepository;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.dto.PreferredCategoryRequest;
import com.devita.domain.user.dto.PreferredCategoryResponse;
import com.devita.domain.user.dto.UserInfoResponse;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RewardRepository rewardRepository;

    @Transactional
    public void updatePreferredCategories(Long userId, PreferredCategoryRequest request) {
        User user = getUserById(userId);
        user.updatePreferredCategories(request.categories());
    }

    public PreferredCategoryResponse getPreferredCategories(Long userId) {
        User user = getUserById(userId);
        return new PreferredCategoryResponse(user.getPreferredCategories());
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = getUserById(userId);
        Reward reward = rewardRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REWARD_NOT_FOUND));

        return new UserInfoResponse(user.getNickname(), reward.getExperience(), reward.getNutrition());
    }
}