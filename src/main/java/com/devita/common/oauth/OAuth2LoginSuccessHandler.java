package com.devita.common.oauth;

import com.devita.common.exception.ErrorCode;
import com.devita.domain.user.repository.UserRepository;
import com.devita.domain.user.domain.User;
import com.devita.common.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    @Value("${front.redirectUrl}") private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        if (email == null) {
            throw new OAuth2AuthenticationException(ErrorCode.TOKEN_NOT_FOUND.getMessage());
        }

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new OAuth2AuthenticationException(ErrorCode.USER_NOT_FOUND.getMessage())
        );

        jwtTokenProvider.createRefreshToken(response, user.getId());

        getRedirectStrategy().sendRedirect(request, response,  redirectUrl);
    }
}