package com.devita.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.secret.access}")
    private String accessTokenSecret;

    @Value("${jwt.secret.refresh}")
    private String refreshTokenSecret;

    @Value("${jwt.expiration.access}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.expiration.refresh}")
    private long refreshTokenValidityInMilliseconds;

    // 액세스 토큰 생성
    public String createAccessToken(Long userId) {

        return createToken(userId, accessTokenValidityInMilliseconds, accessTokenSecret);
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(HttpServletResponse response, Long userId) {
        String refreshToken = createToken(userId, refreshTokenValidityInMilliseconds, refreshTokenSecret);

        storeRefreshToken(response, userId, refreshToken);

        return refreshToken;
    }

    // 토큰 생성
    private String createToken(Long userId, long validity, String secret) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public void storeRefreshToken(HttpServletResponse response, Long userId, String refreshToken){
        // Redis에 리프레시 토큰 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken, refreshTokenValidityInMilliseconds);

        // 쿠키에 리프레시 토큰 저장
        addRefreshTokenCookie(response, refreshToken);

    }

    // 리프레시 토큰 쿠키 생성 및 설정
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .maxAge((int) (refreshTokenValidityInMilliseconds / 1000))
                .sameSite("None")
                .secure(true)
                .path("/")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }


    // 리프레시 토큰 검증
    public String validateRefreshToken(String refreshToken, Long userId) {
        if (refreshTokenService.hasValidRefreshToken(userId, refreshToken)) {
            return createAccessToken(userId);
        }
        throw new IllegalArgumentException("Invalid refresh token");
    }

    // 액세스 토큰 검증
    public boolean validateAccessToken(String token) {
        try {
            log.info("액세스 토큰 검증을 시작합니다: " + token);
            Jwts.parser().setSigningKey(accessTokenSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // 토큰에서 사용자 ID 추출
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(accessTokenSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public String getRole(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(accessTokenSecret)
                .parseClaimsJws(token)
                .getBody();


        return claims.get("role", String.class);
    }

    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(refreshTokenSecret)
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }
}
