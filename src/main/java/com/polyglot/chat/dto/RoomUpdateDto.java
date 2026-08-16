package com.polyglot.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateDto {
    private String roomId;
    private String action; // CREATED, USER_ADDED, USER_REMOVED
    private String initiatedBy;
    private String targetUser;
}