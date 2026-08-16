package com.polyglot.chat.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import com.polyglot.chat.dto.AuthResponse;
import com.polyglot.chat.dto.UserDto;
import com.polyglot.chat.model.User;
import com.polyglot.chat.repository.UserRepository;
import com.polyglot.chat.security.JwtTokenProvider;
import com.polyglot.chat.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final Cloudinary cloudinary;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // ⭐ PURE FIREBASE LOGIN — Twilio Removed
    @Override
    public AuthResponse firebaseLogin(String firebaseToken) throws Exception {

        com.google.firebase.auth.FirebaseToken decodedToken =
                FirebaseAuth.getInstance().verifyIdToken(firebaseToken);

        // Firebase stores phone number under "phone_number" claim (phone auth only)
        String phone = decodedToken.getClaims().containsKey("phone_number")
                ? decodedToken.getClaims().get("phone_number").toString()
                : null;

        String email = decodedToken.getEmail(); // present for Google, null for phone auth
        String name = decodedToken.getName();
        String picture = decodedToken.getPicture();
        String uid = decodedToken.getUid();

        if (phone == null && email == null) {
            throw new RuntimeException("Firebase did not return phone number or email");
        }

        boolean isGoogleLogin = phone == null;

        Optional<User> existingUser = isGoogleLogin
                ? userRepository.findByAuthDetails_ProviderAndAuthDetails_ProviderId(
                User.AuthProvider.GOOGLE, uid)
                : userRepository.findByMobileNumber(phone);

        boolean isNewUser = false;
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            User.UserBuilder builder = User.builder()
                    .username(generateUsername(isGoogleLogin ? email : phone))
                    .displayName(name) // null for phone auth, real name for Google
                    .preferredLanguage("en")
                    .theme("light")
                    .profileCompleted(false)
                    .createdAt(new Date());

            if (isGoogleLogin) {
                builder.email(email)
                        .profilePictureUrl(picture)
                        .authDetails(new User.AuthDetails(User.AuthProvider.GOOGLE, uid));
            } else {
                builder.mobileNumber(phone)
                        .authDetails(new User.AuthDetails(User.AuthProvider.FIREBASE, null));
            }

            user = builder.build();
            user = userRepository.save(user);
            isNewUser = true;
        }

        String jwt = jwtTokenProvider.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(jwt)
                .user(mapUserToDto(user))
                .isNewUser(isNewUser)
                .profileCompleted(user.isProfileCompleted())
                .theme(user.getTheme())
                .build();
    }
    // ⭐ GOOGLE LOGIN (Spring Security OAuth2 redirect flow — separate from Firebase popup login)
    @Override
    public AuthResponse handleOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getName();

        Optional<User> existingUser = userRepository
                .findByAuthDetails_ProviderAndAuthDetails_ProviderId(
                        User.AuthProvider.GOOGLE, googleId);

        boolean isNewUser = false;
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = User.builder()
                    .email(email)
                    .displayName(name)
                    .username(generateUsername(email))
                    .profilePictureUrl(pictureUrl)
                    .preferredLanguage("en")
                    .theme("light")
                    .profileCompleted(false)
                    .authDetails(new User.AuthDetails(User.AuthProvider.GOOGLE, googleId))
                    .createdAt(new Date())
                    .build();

            user = userRepository.save(user);
            isNewUser = true;
        }

        String token = jwtTokenProvider.generateToken(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .user(mapUserToDto(user))
                .isNewUser(isNewUser)
                .profileCompleted(user.isProfileCompleted())
                .theme(user.getTheme())
                .build();
    }

    // ⭐ PROFILE UPDATE + CLOUDINARY UPLOAD
    @Override
    public UserDto updateUserProfile(
            String userId,
            String displayName,
            String username,
            String preferredLanguage,
            String theme,
            MultipartFile image
    ) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDisplayName(displayName);
        user.setPreferredLanguage(preferredLanguage);
        user.setTheme(theme);

        if (!username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(username);
        }

        // Cloudinary upload
        if (image != null && !image.isEmpty()) {
            try {
                Map uploadResult = cloudinary.uploader().upload(
                        image.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "polyglot/profile_images",
                                "resource_type", "image"
                        )
                );

                user.setProfilePictureUrl(uploadResult.get("secure_url").toString());

            } catch (Exception e) {
                throw new RuntimeException("Cloudinary upload failed", e);
            }
        }

        user.setProfileCompleted(true);

        userRepository.save(user);
        return mapUserToDto(user);
    }

    // ⭐ GET LOGGED-IN USER
    @Override
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found with username: " + username));

        return mapUserToDto(user);
    }

    // ⭐ Helpers
    private String generateUsername(String input) {
        String base = input.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.length() > 10) base = base.substring(0, 10);
        return base + (int) (Math.random() * 10000);
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .username(user.getUsername())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .preferredLanguage(user.getPreferredLanguage())
                .contacts(user.getContacts())
                .profileCompleted(user.isProfileCompleted())
                .theme(user.getTheme())
                .build();
    }
}
