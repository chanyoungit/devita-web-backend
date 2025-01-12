package com.devita.domain.user.controller;

import com.devita.common.response.ApiResponse;
import com.devita.domain.user.dto.UserAuthResponse;
import com.devita.domain.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;


    @PostMapping("/user/info")
    public ResponseEntity<ApiResponse<UserAuthResponse>> sendUserInitData(@CookieValue("refreshToken") String refreshToken) {
        log.info("로그인 성공 후 유저 정보를 반환합니다.(액세스 토큰, 닉네임 ...)");
        UserAuthResponse response = authService.refreshUserAuth(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + response.accessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(@CookieValue("refreshToken") String refreshToken) {
        UserAuthResponse response = authService.refreshUserAuth(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + response.accessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success(response.accessToken()));
    }
}
