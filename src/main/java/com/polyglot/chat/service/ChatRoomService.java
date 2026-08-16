package com.polyglot.chat.service;

import com.polyglot.chat.dto.ChatRoomDto;
import com.polyglot.chat.model.ChatRoom;

import java.util.List;

public interface ChatRoomService {
    
    /**
     * Creates a new direct chat room between two users
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return The created chat room
     */
    ChatRoomDto createDirectChatRoom(String userId1, String userId2);
    
    /**
     * Creates a new group chat room
     * 
     * @param name Group name
     * @param creatorId Creator user ID
     * @param participantIds List of participant user IDs
     * @return The created chat room
     */
    ChatRoomDto createGroupChatRoom(String name, String creatorId, List<String> participantIds);
    
    /**
     * Gets all chat rooms for a user
     * 
     * @param userId User ID
     * @return List of chat rooms
     */
    List<ChatRoomDto> getChatRoomsForUser(String userId);
    
    /**
     * Gets a chat room by ID
     * 
     * @param roomId Room ID
     * @return The chat room
     */
    ChatRoomDto getChatRoomById(String roomId);
    
    /**
     * Adds a user to a group chat room
     * 
     * @param roomId Room ID
     * @param userId User ID to add
     * @param addedBy User ID who is adding the new user
     * @return The updated chat room
     */
    ChatRoomDto addUserToGroupChat(String roomId, String userId, String addedBy);
    
    /**
     * Removes a user from a group chat room
     * 
     * @param roomId Room ID
     * @param userId User ID to remove
     * @param removedBy User ID who is removing the user
     * @return The updated chat room
     */
    ChatRoomDto removeUserFromGroupChat(String roomId, String userId, String removedBy);
}