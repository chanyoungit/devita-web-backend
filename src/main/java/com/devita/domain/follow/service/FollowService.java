package com.devita.domain.follow.service;

import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.follow.domain.Follow;
import com.devita.domain.follow.dto.FollowCountDTO;
import com.devita.domain.follow.dto.FollowResponseDTO;
import com.devita.domain.follow.repository.FollowRepository;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new ResourceNotFoundException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        User follower = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        if (isFollowing(userId, targetUserId)) {
            throw new ResourceNotFoundException(ErrorCode.ALREADY_FOLLOWING);
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();

        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long userId, Long targetUserId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FOLLOW_NOT_FOUND));

        followRepository.delete(follow);
    }

    public List<FollowResponseDTO> getFollowings(Long userId) {
        List<Follow> followings = followRepository.findByFollowerId(userId);
        return followings.stream()
                .map(follow -> FollowResponseDTO.from(follow, true))
                .toList();
    }

    public List<FollowResponseDTO> getFollowers(Long userId) {
        List<Follow> followers = followRepository.findByFollowingId(userId);
        return followers.stream()
                .map(follow -> FollowResponseDTO.from(follow, false))
                .toList();
    }

    public boolean isFollowing(Long userId, Long targetUserId) {
        return followRepository.existsByFollowerIdAndFollowingId(userId, targetUserId);
    }

    public FollowCountDTO getFollowCount(Long userId) {
        long followingCount = followRepository.countByFollowerId(userId);
        long followerCount = followRepository.countByFollowingId((userId));
        return new FollowCountDTO(followingCount, followerCount);
    }
}
