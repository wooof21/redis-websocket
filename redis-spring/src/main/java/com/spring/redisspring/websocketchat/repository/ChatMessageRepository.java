package com.spring.redisspring.websocketchat.repository;

import com.spring.redisspring.websocketchat.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, UUID> {

    @Query("SELECT * FROM chat_message " +
            "WHERE room = :room " +
            "ORDER BY timestamp ASC " +
            "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<ChatMessage> findByRoom(String room, Pageable pageable);

}
