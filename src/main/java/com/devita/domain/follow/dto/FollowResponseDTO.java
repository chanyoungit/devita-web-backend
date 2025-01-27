package com.devita.domain.follow.dto;

import com.devita.domain.follow.domain.Follow;
import com.devita.domain.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowResponseDTO {
    private Long userId;
    private String nickname;
    private String profileImage;

    public static FollowResponseDTO from(Follow follow, boolean isFollowing) {
        User user = isFollowing ? follow.getFollowing() : follow.getFollower();
        return FollowResponseDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();
    }
}
