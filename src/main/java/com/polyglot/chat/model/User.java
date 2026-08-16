package com.polyglot.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    private String displayName;
    
    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true, sparse = true)
    private String mobileNumber;
    
    private String email;
    
    private String profilePictureUrl;
    
    @Builder.Default
    private String preferredLanguage = "en";

    @Builder.Default
    private boolean profileCompleted = false;

    @Builder.Default
    private String theme = "light";
    
    private AuthDetails authDetails;
    
    @Builder.Default
    private List<String> contacts = new ArrayList<>();
    
    private Date createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthDetails {
        private AuthProvider provider;
        private String providerId;
    }

    public enum AuthProvider {
        FIREBASE, GOOGLE
    }

}