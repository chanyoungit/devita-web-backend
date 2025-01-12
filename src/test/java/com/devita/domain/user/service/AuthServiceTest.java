package com.devita.domain.user.service;

import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.SecurityTokenException;
import com.devita.common.jwt.JwtTokenProvider;
import com.devita.domain.category.dto.CategoryResDTO;
import com.devita.domain.category.service.CategoryService;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.dto.UserAuthResponse;
import com.devita.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private AuthService authService;

    private static final String REFRESH_TOKEN = "test.refresh.token";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";
    private static final String USER_NICKNAME = "testUser";

    @Test
    @DisplayName("유효한 리프레시 토큰으로 인증 갱신 성공")
    void refreshUserAuth_Success() {
        // given
        User mockUser = User.builder()
                .email(USER_EMAIL)
                .nickname(USER_NICKNAME)
                .provider(AuthProvider.KAKAO)
                .profileImage("profile.jpg")
                .build();
        mockUser.setId(USER_ID);

        List<CategoryResDTO> mockCategories = List.of(
                CategoryResDTO.builder()
                        .id(1L)
                        .name("Category1")
                        .color("#000000")
                        .build()
        );

        when(jwtTokenProvider.getUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN, USER_ID)).thenReturn(NEW_ACCESS_TOKEN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));
        when(categoryService.findUserCategories(USER_ID)).thenReturn(mockCategories);

        // when
        UserAuthResponse response = authService.refreshUserAuth(REFRESH_TOKEN);

        // then
        assertNotNull(response);
        assertEquals(NEW_ACCESS_TOKEN, response.accessToken());
        assertEquals(USER_EMAIL, response.email());
        assertEquals(USER_NICKNAME, response.nickname());
        assertEquals(mockCategories, response.categories());

        verify(jwtTokenProvider).getUserIdFromRefreshToken(REFRESH_TOKEN);
        verify(jwtTokenProvider).validateRefreshToken(REFRESH_TOKEN, USER_ID);
        verify(userRepository).findById(USER_ID);
        verify(categoryService).findUserCategories(USER_ID);
    }

    @Test
    @DisplayName("유효하지 않은 유저로 갱신 시 실패 처리")
    void refreshUserAuth_UserNotFound() {
        // given
        when(jwtTokenProvider.getUserIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.validateRefreshToken(REFRESH_TOKEN, USER_ID)).thenReturn(NEW_ACCESS_TOKEN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        SecurityTokenException exception = assertThrows(SecurityTokenException.class, () -> authService.refreshUserAuth(REFRESH_TOKEN));

        // then
        assertEquals(ErrorCode.INTERNAL_TOKEN_SERVER_ERROR, exception.getErrorCode());
    }
}
