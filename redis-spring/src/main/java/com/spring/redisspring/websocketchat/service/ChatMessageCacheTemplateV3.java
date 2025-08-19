package com.spring.redisspring.websocketchat.service;

import com.example.templates.CacheTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.redisspring.websocketchat.model.ChatMessage;
import com.spring.redisspring.websocketchat.repository.ChatMessageRepository;
import lombok.AllArgsConstructor;
import org.redisson.api.RListReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.StringCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.spring.redisspring.websocketchat.config.Constants.MAX_HISTORY;

@AllArgsConstructor
public class ChatMessageCacheTemplateV3 extends CacheTemplate<UUID, ChatMessage> {

    private final ChatMessageRepository repository;
    private final ObjectMapper jsonMapper; // Spring Boot auto-configures one
    private final RedissonReactiveClient redisson;
    private final String room;

    private RListReactive<String> historyCache() {
        return redisson.getList("history:" + room, StringCodec.INSTANCE);
    }

    private Mono<Void> trimHistory(RListReactive<String> historyCache) {
        return historyCache.size()
                .flatMap(size -> {
                    if (size <= MAX_HISTORY) return Mono.empty();
                    int toRemove = size - MAX_HISTORY;
                    Flux<?> removals = Flux.range(0, toRemove)
                            .concatMap(i -> historyCache.remove(0));
                    return removals.then();
                });
    }

    @Override
    protected Mono<ChatMessage> insertSource(ChatMessage chatMessage) {
        return repository.save(chatMessage);
    }

    @Override
    protected Mono<ChatMessage> insertCache(ChatMessage chatMessage){
        RListReactive<String> history = historyCache();
        try {
            String content = jsonMapper.writeValueAsString(chatMessage);
            return history.add(content)
                    // maintain history size
                    .then(trimHistory(history))
                    .thenReturn(chatMessage);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    @Override
    protected Mono<ChatMessage> getFromSource(UUID uuid) {
        return repository.findById(uuid);
    }

    @Override
    protected Mono<ChatMessage> getFromCache(UUID uuid) {
        return historyCache().readAll()
                .flatMapMany(Flux::fromIterable)
                .flatMap(json -> {
                    try {
                        ChatMessage msg = jsonMapper.readValue(json, ChatMessage.class);
                        return Mono.just(msg);
                    } catch (JsonProcessingException e) {
                        return Mono.empty(); // ignore invalid cache entries
                    }
                })
                .filter(msg -> msg.getId().equals(uuid))
                .next(); // take the first matching one
    }

    @Override
    protected Mono<ChatMessage> updateSource(UUID uuid, ChatMessage chatMessage) {
        chatMessage.setId(uuid);
        return repository.save(chatMessage);
    }

    @Override
    protected Mono<ChatMessage> updateCache(UUID uuid, ChatMessage chatMessage) {
        RListReactive<String> history = historyCache();
        return history.iterator()
                .index()
                .filterWhen(tuple -> {
                    String json = tuple.getT2();
                    try {
                        ChatMessage msg = jsonMapper.readValue(json, ChatMessage.class);
                        return Mono.just(msg.getId().equals(uuid));
                    } catch (JsonProcessingException e) {
                        return Mono.just(false);
                    }
                })
                .next()
                .flatMap(tuple -> history.set(tuple.getT1().intValue(), chatMessage.toString()))
                .thenReturn(chatMessage);
    }

    @Override
    protected Mono<Boolean> deleteFromSource(UUID uuid) {
        return Mono.just(true);
    }

    @Override
    protected Mono<Boolean> deleteFromCache(UUID uuid) {
        return Mono.just(true);
    }
}
