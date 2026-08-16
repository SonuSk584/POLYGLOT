package com.polyglot.chat.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 🚀 SKIP JWT CHECK FOR PUBLIC AUTH ROUTES
        if (path.startsWith("/api/auth/firebase-login")
                || path.startsWith("/api/auth/oauth2")
                || path.startsWith("/api/auth/send-otp")
                || path.startsWith("/api/auth/verify-otp")) {

            log.debug("Skipping JWT filter for public path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = extractJwt(request);

            if (jwt != null && tokenProvider.validateToken(jwt)) {

                String username = tokenProvider.getUserIdFromToken(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwt(HttpServletRequest request) {

        // Header token
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // WebSocket token
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    try {
                        return URLDecoder.decode(param.substring(6), "UTF-8");
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }
}
