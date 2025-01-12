package com.devita.domain.user.dto;

public record UserInfoResponse(
        String nickname,
        int experience,
        int nutrition
) {}
