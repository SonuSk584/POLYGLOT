package com.polyglot.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.Getter;

/**
 * Centralized configuration class for environment variables.
 * Twilio has been removed (Firebase handles OTP).
 */
@Configuration
@Getter
public class EnvironmentConfig {

    // MongoDB Configuration
    @Value("${MONGODB_HOST:${spring.data.mongodb.host:localhost}}")
    private String mongoHost;

    @Value("${MONGODB_PORT:${spring.data.mongodb.port:27017}}")
    private int mongoPort;

    @Value("${MONGODB_DATABASE:${spring.data.mongodb.database:polyglot_chat}}")
    private String mongoDatabase;

    @Value("${MONGODB_URI:}")
    private String mongoUri;

    // JWT Configuration
    @Value("${JWT_SECRET:${jwt.secret:polyglotChatSecretKey2023ForSecureJWTTokenGeneration}}")
    private String jwtSecret;

    @Value("${JWT_EXPIRATION:${jwt.expiration:86400000}}")
    private long jwtExpiration;

    // Google OAuth2 Configuration
    @Value("${GOOGLE_CLIENT_ID:${spring.security.oauth2.client.registration.google.client-id:}}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:${spring.security.oauth2.client.registration.google.client-secret:}}")
    private String googleClientSecret;

    // Hugging Face Translation API
    @Value("${HUGGINGFACE_API_URL:${huggingface.api.url:https://api-inference.huggingface.co/models/facebook/nllb-200-distilled-600M}}")
    private String huggingfaceApiUrl;

    @Value("${HUGGINGFACE_API_TOKEN:${huggingface.api.token:}}")
    private String huggingfaceApiToken;

    // Translation Mock Configuration
    @Value("${TRANSLATION_USE_MOCK:${translation.use.mock:true}}")
    private boolean useMockTranslation;

    /**
     * WebClient bean for external API calls
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    /**
     * Determines if MongoDB should use URI or host/port
     */
    public boolean useMongoUri() {
        return mongoUri != null && !mongoUri.isEmpty();
    }
}
