package com.polyglot.chat.service;

import com.polyglot.chat.dto.ChatMessageDto;
import com.polyglot.chat.model.Message;

import java.util.List;
import java.util.Map;

public interface MessageService {

    /**
     * Saves a message and handles translation
     *
     * @param messageDto The message to save
     * @return The saved message with translations
     */
    ChatMessageDto saveAndTranslateMessage(ChatMessageDto messageDto);

    /**
     * Sends a message from a user to a chat room
     *
     * @param roomId The chat room ID
     * @param senderId The ID of the user sending the message
     * @param content The message content
     * @param sourceLanguage The language of the original message
     * @return The saved message
     */
    ChatMessageDto sendMessage(String roomId, String senderId, String content, String sourceLanguage);

    /**
     * Gets messages for a specific chat room
     *
     * @param roomId The chat room ID
     * @param limit Maximum number of messages to retrieve
     * @return List of messages
     */
    List<ChatMessageDto> getMessagesByRoomId(String roomId, int limit);

    /**
     * Gets messages for a specific chat room before a certain timestamp
     *
     * @param roomId The chat room ID
     * @param timestamp The timestamp to get messages before
     * @param limit Maximum number of messages to retrieve
     * @return List of messages
     */
    List<ChatMessageDto> getMessagesByRoomIdBeforeTimestamp(String roomId, long timestamp, int limit);

    /**
     * Gets all participants in a chat room and their preferred languages
     *
     * @param roomId The chat room ID
     * @return Map of userId -> preferredLanguage
     */
    Map<String, String> getParticipantLanguages(String roomId);
}