package com.devita.domain.post.dto;

import lombok.Builder;

@Builder
public record PostsResDTO(
        Long id,
        String title,
        String description,
        Long likes,
        Long views
) {
}
