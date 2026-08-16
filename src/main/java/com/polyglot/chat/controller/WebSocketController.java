package com.polyglot.chat.controller;

import com.polyglot.chat.dto.ChatMessageDto;
import com.polyglot.chat.dto.ChatRoomDto;
import com.polyglot.chat.dto.RoomUpdateDto;
import com.polyglot.chat.model.ChatRoom;
import com.polyglot.chat.service.ChatRoomService;
import com.polyglot.chat.service.MessageService;
import com.polyglot.chat.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatRoomService chatRoomService;
    private final TranslationService translationService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto messageDto, SimpMessageHeaderAccessor headerAccessor) {
        // Get username from session attributes (set by interceptor)
        String senderId = (String) headerAccessor.getSessionAttributes().get("username");

        log.info("========== WebSocket Message Handler Started ==========");
        log.info("Received message from user: {}", senderId);
        log.info("Room ID: {}", messageDto.getRoomId());
        log.info("Original Text: {}", messageDto.getOriginalText());
        log.info("Original Language: {}", messageDto.getOriginalLanguage());

        if (senderId == null) {
            log.error("❌ Sender ID is null - authentication failed");
            return;
        }

        try {
            messageDto.setSenderId(senderId);

            if (messageDto.getRoomId() == null || messageDto.getRoomId().isEmpty()) {
                log.error("Room ID is null or empty");
                sendErrorToUser(senderId, "Room ID cannot be empty");
                return;
            }

            if (messageDto.getOriginalText() == null || messageDto.getOriginalText().isEmpty()) {
                log.error("Message content is null or empty");
                sendErrorToUser(senderId, "Message content cannot be empty");
                return;
            }

            String sourceLanguage = messageDto.getOriginalLanguage();
            if (sourceLanguage == null || sourceLanguage.isEmpty()) {
                sourceLanguage = "en";
                messageDto.setOriginalLanguage(sourceLanguage);
            }

            log.info("Saving message to database...");

            ChatMessageDto savedMessage = messageService.sendMessage(
                    messageDto.getRoomId(),
                    senderId,
                    messageDto.getOriginalText(),
                    sourceLanguage
            );

            if (savedMessage == null || savedMessage.getId() == null) {
                log.error("Message service returned null or invalid message");
                sendErrorToUser(senderId, "Failed to save message to database");
                return;
            }

            log.info("✓ Message saved with ID: {}", savedMessage.getId());

            Map<String, String> participantLanguages = messageService.getParticipantLanguages(messageDto.getRoomId());
            log.info("Participants and their languages: {}", participantLanguages);

            Set<String> targetLanguages = new HashSet<>(participantLanguages.values());

            Map<String, String> translations = translationService.translateText(
                    savedMessage.getOriginalText(),
                    sourceLanguage,
                    targetLanguages
            );
            log.info("✓ Translations completed for languages: {}", translations.keySet());

            for (Map.Entry<String, String> entry : participantLanguages.entrySet()) {
                String userId = entry.getKey();
                String userLanguage = entry.getValue();

                String translatedText = translations.getOrDefault(userLanguage, savedMessage.getOriginalText());

                ChatMessageDto personalizedMessage = ChatMessageDto.builder()
                        .id(savedMessage.getId())
                        .roomId(savedMessage.getRoomId())
                        .senderId(savedMessage.getSenderId())
                        .senderName(savedMessage.getSenderName())
                        .originalText(translatedText)
                        .originalLanguage(userLanguage)
                        .timestamp(savedMessage.getTimestamp())
                        .translations(translations)
                        .build();

                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/messages",
                        personalizedMessage
                );

                log.info("✓ Sent translated message to user {} in language {}", userId, userLanguage);
            }

            messagingTemplate.convertAndSend(
                    "/topic/room." + messageDto.getRoomId(),
                    savedMessage
            );

            log.info("✓ Message successfully broadcast to room: {}", messageDto.getRoomId());
            log.info("========== WebSocket Message Handler Completed Successfully ==========");

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            sendErrorToUser(senderId, "Failed to process message: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessageLegacy(@DestinationVariable String roomId, @Payload ChatMessageDto messageDto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Legacy endpoint called - redirecting to new handler");
        messageDto.setRoomId(roomId);
        sendMessage(messageDto, headerAccessor);  // ✅ Now matches
    }

    @MessageMapping("/chat.roomUpdate")
    public void roomUpdate(@Payload RoomUpdateDto roomUpdateDto) {
        log.info("Received room update: Action={}, RoomID={}", roomUpdateDto.getAction(), roomUpdateDto.getRoomId());

        try {
            if (roomUpdateDto.getTargetUser() != null && !roomUpdateDto.getTargetUser().isEmpty()) {
                messagingTemplate.convertAndSendToUser(
                        roomUpdateDto.getTargetUser(),
                        "/queue/roomUpdates",
                        roomUpdateDto
                );
                log.info("Room update notification sent to user: {}", roomUpdateDto.getTargetUser());
            }

            if ("USER_ADDED".equals(roomUpdateDto.getAction()) || "USER_REMOVED".equals(roomUpdateDto.getAction())) {
                ChatRoomDto room = chatRoomService.getChatRoomById(roomUpdateDto.getRoomId());

                if (room != null && room.getType() == ChatRoom.ChatRoomType.GROUP) {
                    for (String participantId : room.getParticipants()) {
                        messagingTemplate.convertAndSendToUser(
                                participantId,
                                "/queue/roomUpdates",
                                roomUpdateDto
                        );
                    }
                    log.info("Room update notification sent to all {} participants", room.getParticipants().size());
                }
            }
        } catch (Exception e) {
            log.error("Error processing room update: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.typing")
    public void typingNotification(@Payload Map<String, String> payload, Principal principal) {
        String roomId = payload.get("roomId");
        String userId = principal.getName();

        log.debug("User {} is typing in room {}", userId, roomId);

        try {
            messagingTemplate.convertAndSend(
                    "/topic/room." + roomId + ".typing",
                    Map.of("userId", userId, "isTyping", true)
            );
        } catch (Exception e) {
            log.error("Error broadcasting typing notification: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat.typing/{roomId}/{userId}")
    public void typingNotificationLegacy(
            @DestinationVariable String roomId,
            @DestinationVariable String userId) {

        log.debug("Legacy typing endpoint - User {} is typing in room {}", userId, roomId);
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomId + "/typing",
                Map.of("userId", userId, "isTyping", true)
        );
    }

    private void sendErrorToUser(String userId, String errorMessage) {
        try {
            log.error("Sending error to user {}: {}", userId, errorMessage);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/errors",
                    Map.of(
                            "error", errorMessage,
                            "timestamp", System.currentTimeMillis()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send error to user {}: {}", userId, e.getMessage());
        }
    }
}