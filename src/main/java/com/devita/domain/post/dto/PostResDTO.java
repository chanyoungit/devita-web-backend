package com.devita.domain.post.dto;

import lombok.Builder;

@Builder
public record PostResDTO(
        Long id,
        String writer,
        String title,
        String description,
        Long likes,
        Long views
) {
}
