package com.polyglot.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
public class Message {
    
    @Id
    private String id;
    
    @Indexed
    private String roomId;
    
    private String senderId;
    
    private Date timestamp;
    
    private MessageContent content;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageContent {
        private String originalLanguage;
        private String originalText;
        
        @Builder.Default
        private Map<String, String> translations = new HashMap<>();
    }
}