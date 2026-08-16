package com.polyglot.chat.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CurrentUser extends User {

    private final String id;
    private final String displayName;
    private final String username;
    private final String mobileNumber;
    private final String email;
    private final String profilePictureUrl;
    private final String preferredLanguage;

    public CurrentUser(String id, String username, String displayName, String email,
                       String mobileNumber, String profilePictureUrl, String preferredLanguage) {
        super(username != null ? username : (mobileNumber != null ? mobileNumber : id),
                "", // Empty password since you're using OTP/Google
                true, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        this.id = id;
        this.displayName = displayName;
        this.username = username;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.preferredLanguage = preferredLanguage;
    }
}