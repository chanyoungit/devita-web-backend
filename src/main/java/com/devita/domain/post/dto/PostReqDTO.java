package com.devita.domain.post.dto;

import lombok.Builder;

@Builder
public record PostReqDTO(
        String title,
        String description
) {
}