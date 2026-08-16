package com.polyglot.chat.repository;

import com.polyglot.chat.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);

    // For getting messages without pagination (used in MessageServiceImpl)
    List<Message> findByRoomIdOrderByTimestampDesc(String roomId);

    // Added method for getting the last message in a room (used in ChatRoomServiceImpl)
    Optional<Message> findFirstByRoomIdOrderByTimestampDesc(String roomId);

    // For getting messages before a timestamp
    List<Message> findByRoomIdAndTimestampBeforeOrderByTimestampDesc(String roomId, Date timestamp);

    List<Message> findByRoomIdAndTimestampAfterOrderByTimestamp(String roomId, Date timestamp);

    long countByRoomId(String roomId);
}