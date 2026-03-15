package com.plum.endorsements.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            int status = wrappedResponse.getStatus();

            log.info("HTTP {} {} -> {} ({}ms) [body={}B]",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    durationMs,
                    wrappedResponse.getContentSize());

            wrappedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs") || path.startsWith("/webjars");
    }
}
