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
    private final FollowCacheService followCacheService;

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

        // 팔로우 상태가 변경되었으므로 캐시 갱신
        followCacheService.deleteFollowCache(userId);
    }

    @Transactional
    public void unfollow(Long userId, Long targetUserId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FOLLOW_NOT_FOUND));

        followRepository.delete(follow);

        // 언팔로우 상태가 변경되었으므로 캐시 갱신
        followCacheService.deleteFollowCache(userId);
    }

    public List<FollowResponseDTO> getFollowings_NoCache(Long userId) {
        List<Follow> followings = followRepository.findByFollowerId(userId);
        return followings.stream()
                .map(follow -> FollowResponseDTO.from(follow, true))
                .toList();
    }

    public List<FollowResponseDTO> getFollowers_NoCache(Long userId) {
        List<Follow> followers = followRepository.findByFollowingId(userId);
        return followers.stream()
                .map(follow -> FollowResponseDTO.from(follow, false))
                .toList();
    }


    public List<FollowResponseDTO> getFollowings(Long userId) {
        // 먼저 캐시에서 팔로잉 목록 가져오기
        List<FollowResponseDTO> followings = followCacheService.getFollowingsFromCache(userId);
        if (followings == null) {
            // 캐시가 비어 있으면 DB에서 가져와 캐시에 저장
            List<Follow> followList = followRepository.findByFollowingId(userId);
            followings = followList.stream()
                    .map(follow -> FollowResponseDTO.from(follow, true))
                    .toList();
            followCacheService.cacheFollowings(userId, followings);
        }

        return followings;
    }

    public List<FollowResponseDTO> getFollowers(Long userId) {
        // 먼저 캐시에서 팔로워 목록을 가져옵니다.
        List<FollowResponseDTO> followers = followCacheService.getFollowersFromCache(userId);
        if (followers == null) {
            // 캐시가 비어 있으면 DB에서 가져와 캐시에 저장합니다.
            List<Follow> followList = followRepository.findByFollowerId(userId);
            followers = followList.stream()
                    .map(follow -> FollowResponseDTO.from(follow, false))
                    .toList();
            followCacheService.cacheFollowers(userId, followers);
        }
        return followers;
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
