package com.polyglot.chat.service.impl;

import com.polyglot.chat.dto.ChatMessageDto;
import com.polyglot.chat.model.Message;
import com.polyglot.chat.model.User;
import com.polyglot.chat.repository.ChatRoomRepository;
import com.polyglot.chat.repository.MessageRepository;
import com.polyglot.chat.repository.UserRepository;
import com.polyglot.chat.service.MessageService;
import com.polyglot.chat.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final TranslationService translationService;
    private final SimpMessagingTemplate messagingTemplate; // ✅ ADD THIS

    @Override
    public ChatMessageDto saveAndTranslateMessage(ChatMessageDto messageDto) {
        log.info("Processing message: {}", messageDto);

        try {
            // Get participants' preferred languages
            Set<String> targetLanguages = getParticipantsLanguages(messageDto.getRoomId());
            log.info("Target languages for translation: {}", targetLanguages);

            // Translate the message
            Map<String, String> translations = translationService.translateText(
                    messageDto.getOriginalText(),
                    messageDto.getOriginalLanguage(),
                    targetLanguages
            );
            log.info("Message translated successfully. Translation map: {}", translations);

            // Create and save the message entity
            Message message = new Message();
            message.setRoomId(messageDto.getRoomId());
            message.setSenderId(messageDto.getSenderId());
            message.setTimestamp(new Date());

            Message.MessageContent content = new Message.MessageContent();
            content.setOriginalLanguage(messageDto.getOriginalLanguage());
            content.setOriginalText(messageDto.getOriginalText());
            content.setTranslations(translations);

            message.setContent(content);

            log.info("Saving message to database. Room: {}, Sender: {}, Content: {}",
                    messageDto.getRoomId(), messageDto.getSenderId(), messageDto.getOriginalText());

            Message savedMessage = messageRepository.save(message);
            log.info("Message saved successfully with ID: {}", savedMessage.getId());

            // Update the DTO with saved data
            messageDto.setId(savedMessage.getId());
            messageDto.setTimestamp(savedMessage.getTimestamp());
            messageDto.setTranslations(translations);

            // Add sender name to the DTO
            userRepository.findById(messageDto.getSenderId())
                    .ifPresent(user -> messageDto.setSenderName(user.getDisplayName()));

            // ✅ BROADCAST VIA WEBSOCKET
            try {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + messageDto.getRoomId(),
                        messageDto
                );
                log.info("✓ Message broadcasted to /topic/room/{}", messageDto.getRoomId());
            } catch (Exception e) {
                log.error("❌ Failed to broadcast message via WebSocket: {}", e.getMessage());
                // Don't throw - message is already saved
            }

            return messageDto;
        } catch (Exception e) {
            log.error("Error in saveAndTranslateMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save and translate message: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatMessageDto sendMessage(String roomId, String senderId, String content, String sourceLanguage) {
        log.info("=== SEND MESSAGE SERVICE START ===");
        log.info("Room ID: {}", roomId);
        log.info("Sender ID: {}", senderId);
        log.info("Content: {}", content);
        log.info("Source Language: {}", sourceLanguage);

        try {
            // Validate sender exists
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> {
                        log.error("Sender not found with username: {}", senderId);
                        return new RuntimeException("Sender not found");
                    });
            log.info("Sender validated: {}", sender.getDisplayName());

            // Validate room exists
            chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> {
                        log.error("Room not found with ID: {}", roomId);
                        return new RuntimeException("Room not found");
                    });
            log.info("Room validated");

            // Use provided source language, or fallback to sender's preferred language
            String detectedLanguage = sourceLanguage != null && !sourceLanguage.isEmpty()
                    ? sourceLanguage
                    : (sender.getPreferredLanguage() != null ? sender.getPreferredLanguage() : "en");

            // ✅ Get participants' preferred languages for translation
            Set<String> targetLanguages = getParticipantsLanguages(roomId);
            log.info("Target languages for translation: {}", targetLanguages);

            // ✅ Translate the message
            Map<String, String> translations = new HashMap<>();
            if (!targetLanguages.isEmpty()) {
                try {
                    translations = translationService.translateText(
                            content,
                            detectedLanguage,
                            targetLanguages
                    );
                    log.info("Message translated successfully. Translations: {}", translations);
                } catch (Exception e) {
                    log.error("Translation failed: {}", e.getMessage());
                    // Continue without translations
                }
            }

            // Create message entity
            Message message = new Message();
            message.setRoomId(roomId);
            message.setSenderId(senderId);
            message.setTimestamp(new Date());

            Message.MessageContent messageContent = new Message.MessageContent();
            messageContent.setOriginalLanguage(detectedLanguage);
            messageContent.setOriginalText(content);
            messageContent.setTranslations(translations);

            message.setContent(messageContent);

            Message savedMessage = messageRepository.save(message);
            log.info("Message saved with ID: {}", savedMessage.getId());

            // Create DTO
            ChatMessageDto messageDto = new ChatMessageDto();
            messageDto.setId(savedMessage.getId());
            messageDto.setRoomId(roomId);
            messageDto.setSenderId(senderId);
            messageDto.setSenderName(sender.getDisplayName());
            messageDto.setOriginalText(content);
            messageDto.setOriginalLanguage(detectedLanguage);
            messageDto.setTimestamp(savedMessage.getTimestamp());
            messageDto.setTranslations(translations);

            // ✅ BROADCAST VIA WEBSOCKET TO ALL ROOM PARTICIPANTS
            try {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        messageDto
                );
                log.info("✓ Message broadcasted to /topic/room/{}", roomId);
            } catch (Exception e) {
                log.error("❌ Failed to broadcast message via WebSocket: {}", e.getMessage(), e);
                // Don't throw exception - message is already saved to database
            }

            log.info("=== SEND MESSAGE SERVICE SUCCESS ===");
            return messageDto;

        } catch (Exception e) {
            log.error("=== SEND MESSAGE SERVICE ERROR ===");
            log.error("Error sending message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getParticipantLanguages(String roomId) {
        log.info("Getting participant languages map for room: {}", roomId);

        Map<String, String> participantLanguages = new HashMap<>();

        try {
            chatRoomRepository.findById(roomId).ifPresent(chatRoom -> {
                chatRoom.getParticipants().forEach(participantId -> {
                    userRepository.findById(participantId).ifPresent(user -> {
                        String preferredLanguage = user.getPreferredLanguage();
                        if (preferredLanguage == null || preferredLanguage.isEmpty()) {
                            preferredLanguage = "en";
                        }
                        participantLanguages.put(participantId, preferredLanguage);
                        log.debug("User {} (ID: {}) prefers language: {}",
                                user.getDisplayName(), participantId, preferredLanguage);
                    });
                });
            });

            log.info("Retrieved languages for {} participants in room {}",
                    participantLanguages.size(), roomId);

        } catch (Exception e) {
            log.error("Error getting participant languages for room {}: {}", roomId, e.getMessage(), e);
        }

        return participantLanguages;
    }

    @Override
    public List<ChatMessageDto> getMessagesByRoomId(String roomId, int limit) {
        log.info("Fetching messages for room: {} with limit: {}", roomId, limit);

        List<Message> messages = messageRepository.findByRoomIdOrderByTimestampDesc(roomId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Found {} messages for room: {}", messages.size(), roomId);

        Collections.reverse(messages);
        return convertToDto(messages);
    }

    @Override
    public List<ChatMessageDto> getMessagesByRoomIdBeforeTimestamp(String roomId, long timestamp, int limit) {
        Date date = new Date(timestamp);
        List<Message> messages = messageRepository.findByRoomIdAndTimestampBeforeOrderByTimestampDesc(roomId, date)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        Collections.reverse(messages);
        return convertToDto(messages);
    }

    private Set<String> getParticipantsLanguages(String roomId) {
        Set<String> languages = new HashSet<>();
        languages.add("en");

        chatRoomRepository.findById(roomId).ifPresent(chatRoom -> {
            chatRoom.getParticipants().forEach(participantId -> {
                userRepository.findById(participantId).ifPresent(user -> {
                    if (user.getPreferredLanguage() != null && !user.getPreferredLanguage().isEmpty()) {
                        languages.add(user.getPreferredLanguage());
                    }
                });
            });
        });

        log.info("Participant languages for room {}: {}", roomId, languages);
        return languages;
    }

    private List<ChatMessageDto> convertToDto(List<Message> messages) {
        Map<String, String> userNames = new HashMap<>();

        return messages.stream().map(message -> {
            ChatMessageDto dto = new ChatMessageDto();
            dto.setId(message.getId());
            dto.setRoomId(message.getRoomId());
            dto.setSenderId(message.getSenderId());
            dto.setTimestamp(message.getTimestamp());
            dto.setOriginalLanguage(message.getContent().getOriginalLanguage());
            dto.setOriginalText(message.getContent().getOriginalText());
            dto.setTranslations(message.getContent().getTranslations());

            // Get sender name with caching
            String senderId = message.getSenderId();
            if (!userNames.containsKey(senderId)) {
                userRepository.findById(senderId).ifPresent(user ->
                        userNames.put(senderId, user.getDisplayName())
                );
            }
            dto.setSenderName(userNames.getOrDefault(senderId, "Unknown User"));

            return dto;
        }).collect(Collectors.toList());
    }
}