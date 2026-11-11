package com.gestionacademica.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applies IP limits before request-body validation or controller invocation. */
@Component
public final class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final String RATE_LIMIT_RESPONSE = "{\"error\":\"rate_limit_exceeded\",\"message\":\"Se excedió el límite de solicitudes. Intenta de nuevo más tarde.\"}";
    private final RequestRateLimiter limiter;
    private final ClientIpResolver clientIpResolver;

    public AuthenticationRateLimitFilter(RequestRateLimiter limiter, ClientIpResolver clientIpResolver) {
        this.limiter = limiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.equals("/api/auth/login") && !path.equals("/api/auth/forgot-password")
                && !path.equals("/api/auth/reset-password") && !path.equals("/api/usuarios");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateLimitedOperation operation = operationFor(request.getRequestURI().substring(request.getContextPath().length()));
        try {
            limiter.checkIp(operation, clientIpResolver.resolve(request));
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException exceeded) {
            response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(exceeded.getRetryAfterSeconds()));
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(RATE_LIMIT_RESPONSE);
        }
    }

    private static RateLimitedOperation operationFor(String path) {
        return switch (path) {
            case "/api/auth/login" -> RateLimitedOperation.LOGIN;
            case "/api/auth/forgot-password" -> RateLimitedOperation.FORGOT_PASSWORD;
            case "/api/auth/reset-password" -> RateLimitedOperation.RESET_PASSWORD;
            case "/api/usuarios" -> RateLimitedOperation.REGISTER;
            default -> throw new IllegalArgumentException("Ruta fuera del ámbito del limitador");
        };
    }
}
