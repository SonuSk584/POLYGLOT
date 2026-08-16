package com.polyglot.chat.service.impl;

import com.polyglot.chat.dto.ChatRoomDto;
import com.polyglot.chat.dto.UserDto;
import com.polyglot.chat.model.ChatRoom;
import com.polyglot.chat.model.Message;
import com.polyglot.chat.repository.ChatRoomRepository;
import com.polyglot.chat.repository.MessageRepository;
import com.polyglot.chat.repository.UserRepository;
import com.polyglot.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    @Override
    public ChatRoomDto createDirectChatRoom(String userId1, String recipientPhone) {
        log.info("Creating direct chat room between user {} and phone {}", userId1, recipientPhone);

        // Validate user1 exists (authenticated user - MongoDB ID)
        if (!userRepository.existsById(userId1)) {
            log.error("User {} not found", userId1);
            throw new RuntimeException("User not found: " + userId1);
        }

        // ✅ Find user2 by mobile number (not phoneNumber!)
        String userId2 = userRepository.findByMobileNumber(recipientPhone)
                .map(user -> {
                    log.info("Found user with ID {} for mobile number {}", user.getId(), recipientPhone);
                    return user.getId();
                })
                .orElseThrow(() -> {
                    log.error("User with mobile number {} not found. User may not be registered.", recipientPhone);
                    throw new RuntimeException("User with mobile number " + recipientPhone + " is not registered");
                });

        // ✅ Check if direct chat already exists between these two users
        Optional<ChatRoom> existingRoom = chatRoomRepository.findDirectChatBetweenTwoUsers(userId1, userId2);

        if (existingRoom.isPresent()) {
            log.info("✓ Direct chat already exists: {}", existingRoom.get().getId());

            ChatRoom room = existingRoom.get();

            // Safety check: Ensure both users are in participants
            if (!room.getParticipants().contains(userId1) || !room.getParticipants().contains(userId2)) {
                log.warn("Fixing participants list for room: {}", room.getId());
                room.setParticipants(Arrays.asList(userId1, userId2));
                room = chatRoomRepository.save(room);
            }

            // Update contacts for both users
            updateUserContacts(userId1, userId2);
            updateUserContacts(userId2, userId1);

            return convertToDto(room);
        }

        // ✅ Create new direct chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(ChatRoom.ChatRoomType.DIRECT);
        chatRoom.setParticipants(Arrays.asList(userId1, userId2));
        chatRoom.setCreatedAt(new Date());

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        log.info("✓ Created new direct chat: {} between users {} and {}",
                savedRoom.getId(), userId1, userId2);

        // Update contacts for both users
        updateUserContacts(userId1, userId2);
        updateUserContacts(userId2, userId1);

        return convertToDto(savedRoom);
    }

    /**
     * Updates a user's contacts list by adding another user
     * @param userId The user to update
     * @param contactId The contact to add
     */
    private void updateUserContacts(String userId, String contactId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (!user.getContacts().contains(contactId)) {
                List<String> updatedContacts = new ArrayList<>(user.getContacts());
                updatedContacts.add(contactId);
                user.setContacts(updatedContacts);
                userRepository.save(user);
                log.info("Added user {} to contacts of user {}", contactId, userId);
            }
        });
    }

    @Override
    public ChatRoomDto createGroupChatRoom(String name, String creatorId, List<String> participantIds) {
        log.info("Creating group chat room '{}' by user {}", name, creatorId);

        // Ensure creator is in the participants list
        if (!participantIds.contains(creatorId)) {
            participantIds.add(creatorId);
        }

        // Create new group chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(ChatRoom.ChatRoomType.GROUP);
        chatRoom.setGroupName(name);
        chatRoom.setParticipants(participantIds);
        chatRoom.setAdmins(Collections.singletonList(creatorId));
        chatRoom.setCreatedAt(new Date());

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        log.info("Created new group chat room: {}", savedRoom.getId());

        return convertToDto(savedRoom);
    }

    @Override
    public List<ChatRoomDto> getChatRoomsForUser(String userId) {
        log.info("Getting chat rooms for user {}", userId);

        List<ChatRoom> rooms = chatRoomRepository.findByParticipantsContains(userId);
        return rooms.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ChatRoomDto getChatRoomById(String roomId) {
        log.info("Getting chat room {}", roomId);

        return chatRoomRepository.findById(roomId)
                .map(this::convertToDto)
                .orElse(null);
    }

    @Override
    public ChatRoomDto addUserToGroupChat(String roomId, String userId, String addedBy) {
        log.info("Adding user {} to group chat {} by user {}", userId, roomId, addedBy);

        return chatRoomRepository.findById(roomId)
                .filter(room -> room.getType() == ChatRoom.ChatRoomType.GROUP)
                .filter(room -> room.getAdmins().contains(addedBy))
                .map(room -> {
                    // Add user if not already in the room
                    if (!room.getParticipants().contains(userId)) {
                        List<String> updatedParticipants = new ArrayList<>(room.getParticipants());
                        updatedParticipants.add(userId);
                        room.setParticipants(updatedParticipants);
                        return chatRoomRepository.save(room);
                    }
                    return room;
                })
                .map(this::convertToDto)
                .orElse(null);
    }

    @Override
    public ChatRoomDto removeUserFromGroupChat(String roomId, String userId, String removedBy) {
        log.info("Removing user {} from group chat {} by user {}", userId, roomId, removedBy);

        return chatRoomRepository.findById(roomId)
                .filter(room -> room.getType() == ChatRoom.ChatRoomType.GROUP)
                .filter(room -> room.getAdmins().contains(removedBy) || userId.equals(removedBy))
                .map(room -> {
                    // Remove user from participants
                    if (room.getParticipants().contains(userId)) {
                        List<String> updatedParticipants = new ArrayList<>(room.getParticipants());
                        updatedParticipants.remove(userId);
                        room.setParticipants(updatedParticipants);

                        // Also remove from admins if they were an admin
                        if (room.getAdmins().contains(userId)) {
                            List<String> updatedAdmins = new ArrayList<>(room.getAdmins());
                            updatedAdmins.remove(userId);
                            room.setAdmins(updatedAdmins);
                        }

                        return chatRoomRepository.save(room);
                    }
                    return room;
                })
                .map(this::convertToDto)
                .orElse(null);
    }

    private ChatRoomDto convertToDto(ChatRoom chatRoom) {
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(chatRoom.getId());
        dto.setType(chatRoom.getType());
        dto.setParticipants(chatRoom.getParticipants());
        dto.setGroupName(chatRoom.getGroupName());
        dto.setGroupIconUrl(chatRoom.getGroupIconUrl());
        dto.setAdmins(chatRoom.getAdmins());
        dto.setCreatedAt(chatRoom.getCreatedAt());

        // Get participant details
        List<UserDto> participantDetails = chatRoom.getParticipants().stream()
                .map(userId -> userRepository.findById(userId)
                        .map(user -> {
                            UserDto userDto = new UserDto();
                            userDto.setId(user.getId());
                            userDto.setDisplayName(user.getDisplayName());
                            userDto.setUsername(user.getUsername());
                            userDto.setProfilePictureUrl(user.getProfilePictureUrl());
                            userDto.setPreferredLanguage(user.getPreferredLanguage());
                            return userDto;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        dto.setParticipantDetails(participantDetails);

        // Get last message preview
        messageRepository.findFirstByRoomIdOrderByTimestampDesc(chatRoom.getId())
                .ifPresent(message -> {
                    dto.setLastMessagePreview(message.getContent().getOriginalText());
                    dto.setLastMessageTimestamp(message.getTimestamp());
                });

        return dto;
    }
}
