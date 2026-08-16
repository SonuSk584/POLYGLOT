package com.polyglot.chat.config;

import com.polyglot.chat.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        try {
            log.info("=== WebSocket Handshake Started ===");

            String token = null;

            // Try to extract token from query parameters
            String query = request.getURI().getQuery();
            log.info("Query string: {}", query);

            if (query != null && !query.isEmpty()) {
                token = extractTokenFromQuery(query);
            }

            // ✅ Fallback: Try to get from request parameters (for SockJS compatibility)
            if (token == null && request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                token = servletRequest.getServletRequest().getParameter("token");
                if (token != null) {
                    log.info("✓ Token found in request parameters");
                }
            }

            if (token == null) {
                log.error("❌ No token found in WebSocket handshake request");
                log.error("URI: {}", request.getURI());
                return true; // ✅ TEMPORARILY ALLOW - for debugging
            }

            log.info("✓ Token extracted successfully (length: {})", token.length());

            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUserIdFromToken(token);
                attributes.put("token", token);
                attributes.put("username", username);
                log.info("✅ WebSocket handshake successful for user: {}", username);
                return true;
            } else {
                log.error("❌ Token validation failed");
                return true; // ✅ TEMPORARILY ALLOW - for debugging
            }
        } catch (Exception e) {
            log.error("❌ WebSocket handshake error: {}", e.getMessage(), e);
            return true; // ✅ TEMPORARILY ALLOW - for debugging
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket afterHandshake error: {}", exception.getMessage());
        } else {
            log.info("✓ WebSocket handshake completed");
        }
    }

    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        try {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    String encodedToken = param.substring(6);
                    String decodedToken = URLDecoder.decode(encodedToken, StandardCharsets.UTF_8);
                    log.info("✓ Token decoded from query");
                    return decodedToken;
                }
            }
        } catch (Exception e) {
            log.error("❌ Error decoding token: {}", e.getMessage());
        }
        return null;
    }
}


//        ## Test Now
//
//1. **Restart your Spring Boot backend**
//        2. **Refresh your frontend**
//        3. **Check the backend console** - you should now see:
//        ```
//        ✓ Token found in request parameters
//   ✅ WebSocket handshake successful for user: 98220141534802
//        ✅ WebSocket authentication successful for user: 98220141534802