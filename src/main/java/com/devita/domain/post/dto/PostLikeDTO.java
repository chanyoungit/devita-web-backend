package com.devita.domain.post.dto;

import lombok.Builder;

@Builder
public record PostLikeDTO(
        Long postId,
        Long likeCount
) {
}
