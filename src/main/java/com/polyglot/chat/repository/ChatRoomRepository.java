package com.polyglot.chat.repository;

import com.polyglot.chat.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    List<ChatRoom> findByParticipantsContains(String userId);

    // For finding direct chats between two users
    @Query("{ 'type': ?0, 'participants': { $all: ?1, $size: ?2 } }")
    List<ChatRoom> findByTypeAndParticipantsContainsAllAndParticipantsSize(
            ChatRoom.ChatRoomType type,
            List<String> participants,
            int size
    );

    // ✅ ADD THIS: Simpler method for direct chat lookup
    @Query("{ 'type': 'DIRECT', 'participants': { $all: [?0, ?1], $size: 2 } }")
    Optional<ChatRoom> findDirectChatBetweenTwoUsers(String userId1, String userId2);

    List<ChatRoom> findByTypeAndAdminsContaining(ChatRoom.ChatRoomType type, String userId);
}