package com.polyglot.chat.controller;

import com.polyglot.chat.dto.ChatMessageDto;
import com.polyglot.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles messages sent to /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void handleChatMessage(@Payload Map<String, Object> payload, Authentication authentication) {
        try {
            log.info("📨 WebSocket message received from: {}", authentication.getName());
            log.info("Payload: {}", payload);

            String roomId = (String) payload.get("roomId");
            String senderId = (String) payload.get("senderId");
            String content = (String) payload.get("content");
            String sourceLanguage = (String) payload.get("sourceLanguage");

            if (roomId == null || senderId == null || content == null) {
                log.error("❌ Missing required fields in message payload");
                return;
            }

            log.info("Processing message - Room: {}, Sender: {}, Content: {}", roomId, senderId, content);

            // Save message to database and translate
            ChatMessageDto savedMessage = messageService.sendMessage(roomId, senderId, content, sourceLanguage);

            log.info("✅ Message saved with ID: {}", savedMessage.getId());

            // Message is already broadcasted in MessageServiceImpl
            // But if you removed that, uncomment below:
            // messagingTemplate.convertAndSend("/topic/room/" + roomId, savedMessage);

        } catch (Exception e) {
            log.error("❌ Error handling WebSocket message: {}", e.getMessage(), e);
        }
    }
}
