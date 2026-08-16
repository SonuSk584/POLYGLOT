package com.polyglot.chat.dto;

import com.polyglot.chat.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String username;
    private String displayName;
    private String email;
    private String mobileNumber;
    private String profilePictureUrl;
    private String preferredLanguage;
    private List<String> contacts;
    private boolean profileCompleted;
    private String theme;


    /**
     * Constructor that converts User entity to UserDto
     */
    public UserDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.displayName = user.getDisplayName();
            this.email = user.getEmail();
            this.mobileNumber = user.getMobileNumber();
            this.profilePictureUrl = user.getProfilePictureUrl();
            this.preferredLanguage = user.getPreferredLanguage();
            this.contacts = user.getContacts() != null ? new ArrayList<>(user.getContacts()) : new ArrayList<>();
            this.profileCompleted = user.isProfileCompleted();     // <-- ADD THIS
            this.theme = user.getTheme();                         // <-- ADD THIS
        }
    }

}