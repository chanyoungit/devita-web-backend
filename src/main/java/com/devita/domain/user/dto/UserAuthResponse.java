package com.devita.domain.user.dto;

import com.devita.domain.category.dto.CategoryResDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record UserAuthResponse(
        String accessToken,
        String email,
        String nickname,
        List<CategoryResDTO> categories
) {}