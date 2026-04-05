package com.isums.aiservice.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    @Value("${ai.internal.enabled:true}")
    private boolean enabled;

    @Value("${ai.internal.token:}")
    private String token;

    @Value("${ai.internal.publicPaths:/api/ai/scoring/health}")
    private String publicPaths;

    private final AntPathMatcher matcher = new AntPathMatcher();

    private boolean isPublicPath(String path) {
        List<String> pubs = Arrays.stream(publicPaths.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        for (String p : pubs) {
            if (matcher.match(p, path) || p.equals(path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(req, res);
            return;
        }

        String path = req.getRequestURI();

        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        if (isPublicPath(path)) {
            chain.doFilter(req, res);
            return;
        }

        if (!path.startsWith("/api/ai/")) {
            chain.doFilter(req, res);
            return;
        }

        String auth = req.getHeader("Authorization");
        String expected = "Bearer " + token;

        if (token == null || token.isBlank()) {
            res.setStatus(500);
            res.getWriter().write("ai.internal.token is empty");
            return;
        }

        if (auth == null || !auth.equals(expected)) {
            res.setStatus(401);
            res.getWriter().write("unauthorized");
            return;
        }

        chain.doFilter(req, res);
    }
}
