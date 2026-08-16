package com.polyglot.chat.config;

import com.polyglot.chat.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("=== Registering STOMP endpoints ===");
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000")
                .addInterceptors(new WebSocketHandshakeInterceptor(jwtTokenProvider))
                .withSockJS();
        log.info("✓ STOMP endpoint /ws registered with SockJS");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("=== Configuring message broker ===");
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
        log.info("✓ Message broker configured");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("=== Configuring client inbound channel ===");
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("=== WebSocket CONNECT received ===");

                    // ✅ Get token from session attributes (set by HandshakeInterceptor)
                    String token = (String) accessor.getSessionAttributes().get("token");
                    String username = (String) accessor.getSessionAttributes().get("username");

                    log.info("Session token available: {}", token != null);
                    log.info("Session username: {}", username);

                    if (token != null && username != null) {
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            accessor.setUser(authentication);
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.info("✅ WebSocket authentication successful for user: {}", username);
                        } catch (Exception e) {
                            log.error("❌ Failed to authenticate user: {}", e.getMessage());
                            return null;
                        }
                    } else {
                        log.error("❌ No token or username in session attributes");
                        return null;
                    }
                }

                // Handle SEND and SUBSCRIBE commands
                if (accessor != null && (StompCommand.SEND.equals(accessor.getCommand()) ||
                        StompCommand.SUBSCRIBE.equals(accessor.getCommand()))) {

                    if (accessor.getUser() == null) {
                        String username = (String) accessor.getSessionAttributes().get("username");
                        if (username != null) {
                            try {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities()
                                        );
                                accessor.setUser(authentication);
                            } catch (Exception e) {
                                log.error("Error setting user for {} command: {}", accessor.getCommand(), e.getMessage());
                            }
                        }
                    }
                }

                return message;
            }
        });
        log.info("✓ Client inbound channel configured");
    }
}