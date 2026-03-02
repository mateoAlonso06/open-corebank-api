package com.banking.system.auth.infraestructure.adapter.out.filter;

import com.banking.system.auth.infraestructure.config.CookieHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsrfTokenFilter extends OncePerRequestFilter {

    private final CookieHelper cookieHelper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkipCsrf(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> csrfCookie = cookieHelper.extractCsrfToken(request);
        String csrfHeader = request.getHeader(CookieHelper.CSRF_TOKEN_HEADER);

        if (csrfCookie.isEmpty() || csrfHeader == null || csrfHeader.isBlank()) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Missing CSRF token");
            return;
        }

        if (!csrfCookie.get().equals(csrfHeader)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "CSRF token mismatch");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipCsrf(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        // Only enforce CSRF on requests that carry the refresh_token cookie
        return cookieHelper.extractRefreshToken(request).isEmpty();
    }
}
