package com.polyglot.chat.controller;

import com.polyglot.chat.dto.AuthResponse;
import com.polyglot.chat.dto.UserDto;
import com.polyglot.chat.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 🚀 Firebase Phone Number Login
    @PostMapping("/firebase-login")
    public ResponseEntity<AuthResponse> firebaseLogin(@RequestBody Map<String, Object> request) throws Exception {

        System.out.println("🔥 Incoming Firebase Login Body: " + request);

        Object tokenObj = request.get("firebaseToken");

        if (tokenObj == null) {
            throw new IllegalArgumentException("firebaseToken is required");
        }

        // Convert safely to string even if it looks like an object
        String firebaseToken = tokenObj.toString();

        AuthResponse response = authService.firebaseLogin(firebaseToken);
        return ResponseEntity.ok(response);
    }


    // ✔ Get currently authenticated user
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    // ✔ Update user profile (supports image upload via Cloudinary)
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,

            @RequestPart("displayName") String displayName,
            @RequestPart("username") String username,
            @RequestPart("preferredLanguage") String preferredLanguage,
            @RequestPart("theme") String theme,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        UserDto updatedUser = authService.updateUserProfile(
                userDetails.getUsername(),
                displayName,
                username,
                preferredLanguage,
                theme,
                image
        );

        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<String> oauthSuccess(@RequestParam String token) {
        return ResponseEntity.ok("Login successful. Please close this window and return to the app.");
    }
}
