package com.polyglot.chat.controller;

import com.polyglot.chat.dto.ChatMessageDto;
import com.polyglot.chat.dto.ChatRoomDto;
import com.polyglot.chat.security.CurrentUser;
import com.polyglot.chat.service.ChatRoomService;
import com.polyglot.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getUserChatRooms(@AuthenticationPrincipal CurrentUser currentUser) {
        log.info("Getting chat rooms for user {}", currentUser.getId());
        List<ChatRoomDto> rooms = chatRoomService.getChatRoomsForUser(currentUser.getId());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDto> getChatRoomById(
            @PathVariable String roomId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("User {} requesting chat room {}", currentUser.getId(), roomId);
        ChatRoomDto room = chatRoomService.getChatRoomById(roomId);

        // Check if user is a participant
        if (room != null && room.getParticipants().contains(currentUser.getId())) {
            return ResponseEntity.ok(room);
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping("/rooms/direct/{userId}")
    public ResponseEntity<ChatRoomDto> createDirectChat(
            @PathVariable String userId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("Creating direct chat between {} and {}", currentUser.getId(), userId);
        ChatRoomDto room = chatRoomService.createDirectChatRoom(currentUser.getId(), userId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/rooms/group")
    public ResponseEntity<ChatRoomDto> createGroupChat(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CurrentUser currentUser) {

        String name = (String) request.get("name");
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) request.get("participants");

        log.info("Creating group chat '{}' with participants {}", name, participants);
        ChatRoomDto room = chatRoomService.createGroupChatRoom(name, currentUser.getId(), participants);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/rooms/{roomId}/add/{userId}")
    public ResponseEntity<ChatRoomDto> addUserToGroup(
            @PathVariable String roomId,
            @PathVariable String userId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("User {} adding user {} to room {}", currentUser.getId(), userId, roomId);
        ChatRoomDto room = chatRoomService.addUserToGroupChat(roomId, userId, currentUser.getId());

        if (room != null) {
            return ResponseEntity.ok(room);
        }

        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/rooms/{roomId}/remove/{userId}")
    public ResponseEntity<ChatRoomDto> removeUserFromGroup(
            @PathVariable String roomId,
            @PathVariable String userId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("User {} removing user {} from room {}", currentUser.getId(), userId, roomId);
        ChatRoomDto room = chatRoomService.removeUserFromGroupChat(roomId, userId, currentUser.getId());

        if (room != null) {
            return ResponseEntity.ok(room);
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("User {} requesting messages for room {}", currentUser.getId(), roomId);

        // Check if user is a participant in the room
        ChatRoomDto room = chatRoomService.getChatRoomById(roomId);
        if (room == null || !room.getParticipants().contains(currentUser.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<ChatMessageDto> messages = messageService.getMessagesByRoomId(roomId, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/rooms/{roomId}/messages/before/{timestamp}")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessagesBefore(
            @PathVariable String roomId,
            @PathVariable long timestamp,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("User {} requesting messages for room {} before {}", currentUser.getId(), roomId, timestamp);

        // Check if user is a participant in the room
        ChatRoomDto room = chatRoomService.getChatRoomById(roomId);
        if (room == null || !room.getParticipants().contains(currentUser.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<ChatMessageDto> messages = messageService.getMessagesByRoomIdBeforeTimestamp(roomId, timestamp, limit);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable String roomId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CurrentUser currentUser) {

        String content = (String) request.get("content");
        String targetLanguage = (String) request.get("targetLanguage");

        log.info("User {} sending message to room {}", currentUser.getId(), roomId);

        // Verify user is a participant in the room
        ChatRoomDto room = chatRoomService.getChatRoomById(roomId);
        if (room == null || !room.getParticipants().contains(currentUser.getId())) {
            return ResponseEntity.notFound().build();
        }

        ChatMessageDto message = messageService.sendMessage(roomId, currentUser.getId(), content, targetLanguage);
        return ResponseEntity.ok(message);
    }
}