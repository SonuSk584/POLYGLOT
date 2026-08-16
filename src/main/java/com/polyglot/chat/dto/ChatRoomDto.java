package com.polyglot.chat.dto;

import com.polyglot.chat.model.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private String id;
    private ChatRoom.ChatRoomType type;
    private List<String> participants;
    private List<UserDto> participantDetails;
    private String groupName;
    private String groupIconUrl;
    private List<String> admins;
    private Date createdAt;
    private String lastMessagePreview;
    private Date lastMessageTimestamp;
}