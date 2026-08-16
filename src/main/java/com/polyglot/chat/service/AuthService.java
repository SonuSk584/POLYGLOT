package com.polyglot.chat.service;

import com.polyglot.chat.dto.AuthResponse;
import com.polyglot.chat.dto.UserDto;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    // 🚀 Firebase Phone Auth
    AuthResponse firebaseLogin(String firebaseToken) throws Exception;

    // ✔ Google OAuth
    AuthResponse handleOAuth2User(OAuth2User oAuth2User);

    // ✔ Update profile
    UserDto updateUserProfile(
            String userId,
            String displayName,
            String username,
            String preferredLanguage,
            String theme,
            MultipartFile image
    );

    // ✔ Get logged-in user from JWT
    UserDto getCurrentUser(String userId);
}
