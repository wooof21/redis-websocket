package com.spring.redisspring.websocketchat.service;

import com.example.templates.CacheTemplate;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.redisspring.websocketchat.model.ChatMessage;
import com.spring.redisspring.websocketchat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;

/**
 * May not need to insert every single message into cache
 */
@Service
@RequiredArgsConstructor
public class ChatMessageCacheTemplateV2 extends CacheTemplate<UUID, ChatMessage> {

    private final RedissonReactiveClient redissonClient;
    private final ChatMessageRepository repository;
    private final ObjectMapper jsonMapper; // Spring Boot auto-configures one

    private String redisKey(UUID id) {
        return "chat_message:" + id.toString();
    }

    @Override
    protected Mono<ChatMessage> insertSource(ChatMessage chatMessage) {
        return repository.save(chatMessage);
    }

    @Override
    protected Mono<ChatMessage> insertCache(ChatMessage chatMessage) {
        try {
            String json = jsonMapper.writeValueAsString(chatMessage);
            return redissonClient.getBucket(redisKey(chatMessage.getId()), StringCodec.INSTANCE)
                    .set(json)
                    .thenReturn(chatMessage);
        } catch (JsonProcessingException e) {
            return Mono.just(chatMessage);
        }
    }

    @Override
    protected Mono<ChatMessage> getFromSource(UUID id) {
        return repository.findById(id);
    }

    @Override
    protected Mono<ChatMessage> getFromCache(UUID id) {
        return redissonClient.getBucket(redisKey(id), StringCodec.INSTANCE)
                .get()
                .flatMap(json -> {
                    try {
                        return Mono.just(jsonMapper.readValue((JsonParser) json, ChatMessage.class));
                    } catch (IOException e) {
                        return Mono.empty();
                    }
                });
    }

    @Override
    protected Mono<ChatMessage> updateSource(UUID id, ChatMessage chatMessage) {
        return repository.save(chatMessage);
    }

    @Override
    protected Mono<ChatMessage> updateCache(UUID id, ChatMessage chatMessage) {
        try {
            String json = jsonMapper.writeValueAsString(chatMessage);
            return redissonClient.getBucket(redisKey(id), StringCodec.INSTANCE)
                    .set(json)
                    .thenReturn(chatMessage);
        } catch (JsonProcessingException e) {
            return Mono.just(chatMessage);
        }
    }

    @Override
    protected Mono<Boolean> deleteFromSource(UUID id) {
        return Mono.just(true);
    }

    @Override
    protected Mono<Boolean> deleteFromCache(UUID id) {
        return Mono.just(true);
    }
}
