package com.devita.domain.character.controller;

import com.devita.common.response.ApiResponse;
import com.devita.domain.character.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/character")
public class CharacterController {
    private final RewardService rewardService;

    @PostMapping("/use/nutrition")
    public ApiResponse<Long> useNutrition(@AuthenticationPrincipal Long userId){
        return ApiResponse.success(rewardService.useNutrition(userId));
    }
}
