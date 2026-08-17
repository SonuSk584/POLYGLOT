package com.polyglot.chat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    private static final String RENDER_SECRET_PATH =
            "/etc/secrets/firebase-service-account.json";

    private static final String LOCAL_SECRET_PATH =
            "src/main/resources/firebase-service-account.json";

    @PostConstruct
    public void init() throws Exception {

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        InputStream serviceAccountStream;

        // Render / production
        if (new java.io.File(RENDER_SECRET_PATH).exists()) {

            log.info("🔑 Loading Firebase credentials from Render Secret File");

            serviceAccountStream =
                    new FileInputStream(RENDER_SECRET_PATH);

        } else {

            // Local development
            log.info("🔑 Loading Firebase credentials from local file");

            serviceAccountStream =
                    new FileInputStream(LOCAL_SECRET_PATH);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(
                        GoogleCredentials.fromStream(serviceAccountStream)
                )
                .build();

        FirebaseApp.initializeApp(options);

        serviceAccountStream.close();

        log.info("✅ Firebase initialized successfully");
    }
}