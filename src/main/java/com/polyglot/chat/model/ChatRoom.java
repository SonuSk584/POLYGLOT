package com.polyglot.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatRooms")
public class ChatRoom {
    
    @Id
    private String id;
    
    private ChatRoomType type;
    
    @Builder.Default
    private List<String> participants = new ArrayList<>();
    
    private String groupName;
    
    private String groupIconUrl;
    
    @Builder.Default
    private List<String> admins = new ArrayList<>();
    
    private Date createdAt;
    
    public enum ChatRoomType {
        DIRECT, GROUP
    }
}