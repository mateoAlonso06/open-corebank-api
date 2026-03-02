package com.banking.system.auth.infraestructure.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Component
public class CookieHelper {
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String CSRF_TOKEN_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_TOKEN_HEADER = "X-XSRF-TOKEN";
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final int MAX_AGE_SECONDS = 7 * 24 * 60 * 60;
    private static final int CSRF_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${cookie.secure:true}")
    private boolean secure;

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(MAX_AGE_SECONDS)
                .sameSite("lax")
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite("lax")
                .build();
    }

    public ResponseCookie createCsrfTokenCookie() {
        byte[] bytes = new byte[CSRF_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        return ResponseCookie.from(CSRF_TOKEN_COOKIE, csrfToken)
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .maxAge(MAX_AGE_SECONDS)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie clearCsrfTokenCookie() {
        return ResponseCookie.from(CSRF_TOKEN_COOKIE, "")
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    public Optional<String> extractCsrfToken(HttpServletRequest request) {
        return extractCookieValue(request, CSRF_TOKEN_COOKIE);
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue())
                        .filter(v -> !v.isBlank());
            }
        }
        return Optional.empty();
    }
}
