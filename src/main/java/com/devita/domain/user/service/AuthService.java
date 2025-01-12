package com.devita.domain.user.service;

import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.SecurityTokenException;
import com.devita.common.jwt.JwtTokenProvider;
import com.devita.domain.category.dto.CategoryResDTO;
import com.devita.domain.category.service.CategoryService;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.dto.UserAuthResponse;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public UserAuthResponse refreshUserAuth(String refreshToken) {
        try {
            log.info("액세스 토큰을 생성합니다.");
            Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
            String newAccessToken = jwtTokenProvider.validateRefreshToken(refreshToken, userId);
            log.info("액세스 토큰: " + newAccessToken);
            // 사용자 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new SecurityTokenException(ErrorCode.USER_NOT_FOUND));

            // 카테고리 정보 조회
            List<CategoryResDTO> categories = categoryService.findUserCategories(userId);

            // 응답 데이터 생성
            return UserAuthResponse.builder()
                    .accessToken(newAccessToken)
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .categories(categories)
                    .build();

        } catch (Exception e) {
            log.error("Failed to refresh user authentication: {}", e.getMessage());
            throw new SecurityTokenException(ErrorCode.INTERNAL_TOKEN_SERVER_ERROR);
        }
    }
}