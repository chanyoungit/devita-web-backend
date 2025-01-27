package com.devita.domain.follow.controller;

import com.devita.common.response.ApiResponse;
import com.devita.domain.follow.dto.FollowCountDTO;
import com.devita.domain.follow.dto.FollowResponseDTO;
import com.devita.domain.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follows")
@Slf4j
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{targetUserId}")
    public ApiResponse<String> follow(@AuthenticationPrincipal Long userId, @PathVariable Long targetUserId) {
        followService.follow(userId, targetUserId);
        return ApiResponse.success("성공");
    }

    @DeleteMapping("/{targetUserId}")
    public ApiResponse<String> unfollow(@AuthenticationPrincipal Long userId, @PathVariable Long targetUserId) {
        followService.unfollow(userId, targetUserId);
        return ApiResponse.success("성공");
    }

    @GetMapping("/followings/{userId}")
    public ApiResponse<List<FollowResponseDTO>> getFollowings(@PathVariable Long userId) {
        List<FollowResponseDTO> followings = followService.getFollowings(userId);
        return ApiResponse.success(followings);
    }

    @GetMapping("/followers/{userId}")
    public ApiResponse<List<FollowResponseDTO>> getFollowers(@PathVariable Long userId) {
        List<FollowResponseDTO> followers = followService.getFollowers(userId);
        return ApiResponse.success(followers);
    }

    @GetMapping("/check/{targetUserId}")
    public ApiResponse<Boolean> isFollowing(@AuthenticationPrincipal Long userId, @PathVariable Long targetUserId) {
        boolean isFollowing = followService.isFollowing(userId, targetUserId);
        return ApiResponse.success(isFollowing);
    }

    @GetMapping("/count/{userId}")
    public ApiResponse<FollowCountDTO> getFollowCount(@PathVariable Long userId) {
        FollowCountDTO countDto = followService.getFollowCount(userId);
        return ApiResponse.success(countDto);
    }
}
