package com.polyglot.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private Date timestamp;
    private String originalLanguage;
    private String originalText;
    
    @Builder.Default
    private Map<String, String> translations = new HashMap<>();
}