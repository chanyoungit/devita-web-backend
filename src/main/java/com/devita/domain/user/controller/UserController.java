package com.devita.domain.user.controller;

import com.devita.common.response.ApiResponse;
import com.devita.domain.user.dto.PreferredCategoryRequest;
import com.devita.domain.user.dto.PreferredCategoryResponse;
import com.devita.domain.user.dto.UserInfoResponse;
import com.devita.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    @PutMapping("/preferred-categories")
    public ApiResponse<Void> updatePreferredCategories(@AuthenticationPrincipal Long userId, @RequestBody PreferredCategoryRequest request) {
        userService.updatePreferredCategories(userId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/preferred-categories")
    public ApiResponse<PreferredCategoryResponse> getPreferredCategories(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getPreferredCategories(userId));
    }

    @GetMapping
    public ApiResponse<UserInfoResponse> getUserInfo(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getUserInfo(userId));
    }
}