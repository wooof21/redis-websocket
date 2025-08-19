package com.spring.redisspring.websocketchat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.redisspring.websocketchat.model.ChatMessage;
import com.spring.redisspring.websocketchat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RListReactive;
import org.redisson.api.RTopicReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.spring.redisspring.websocketchat.config.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomServiceV3 implements WebSocketHandler {

    private final RedissonReactiveClient redissonClient;
    private final ChatMessageRepository repository;
    private final ObjectMapper jsonMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {


        Map<String, String> params = getQueryParams(session);
        String room = params.getOrDefault("room", "default");
        String user = params.getOrDefault("user", "anonymous");
        boolean includeHistory = Boolean.parseBoolean(params.getOrDefault("includeHistory", "true"));
        log.info("User - {} - room - {} - includeHistory: {}", user, room, includeHistory);

        RTopicReactive topic = redissonClient.getTopic(room, StringCodec.INSTANCE);
        RListReactive<String> historyCache = redissonClient.getList("history:" + room, StringCodec.INSTANCE);
        ChatMessageCacheTemplateV3 cacheTemplate =
                new ChatMessageCacheTemplateV3(repository, jsonMapper, redissonClient, room);


        Mono<Void> subscriber = session.receive()
                .takeUntilOther(session.closeStatus()) // complete when clients closed
                .map(WebSocketMessage::getPayloadAsText)
                .onBackpressureDrop(msg ->
                        log.warn("[{}] - Dropped incoming msg for user - {} due to backpressure", room, user))
                .flatMap(msgText -> {
                    try {
                        JsonNode json = jsonMapper.readTree(msgText);
                        String type = json.get("type").asText();
                        UUID id = json.get("id") != null ? UUID.fromString(json.get("id").asText()) : null;

                        if ("CHAT_MESSAGE".equals(type)) {
                            return handleChatMessage(json, id, room, user, topic, cacheTemplate);
                        } else if ("LOAD_HISTORY".equals(type)) {
                            return handleLoadHistory(json, session, room);
                        } else {
                            return Mono.empty();
                        }
                    } catch (JsonProcessingException e) {
                        return Mono.empty();
                    }
                }, MAX_IN_FLIGHT_WRITES)
                .then()
                .onErrorResume(ex -> {
                    log.error("[{}] - Subscriber error: {}", room, ex.getMessage(), ex);
                    return Mono.empty(); // continue processing future messages
                })
                .doOnSubscribe(s -> log.info("User ({}) joined room - [{}]", user, room))
                .doFinally(s -> log.info("Subscriber Finally: {} ", s));

        Flux<String> publisher = topic.getMessages(String.class);
        if(includeHistory) {
            publisher = publisher.startWith(historyCache.iterator());
        }

        // Publisher: push messages from Redis pub/sub
        Flux<WebSocketMessage> messageFlux =
                publisher
                    .onBackpressureBuffer(
                            SEND_BUFFER,
                            dropped -> log.warn("Dropping oldest messages for room '{}' - Dropped: {}", room, dropped),
                            BufferOverflowStrategy.DROP_OLDEST
                    )
                    .map(session::textMessage)
                    .doOnSubscribe(s -> log.info("[{}] - Publisher registered for User ({})", room, user))
                    .doFinally(s -> log.info("Publisher Finally: {} ", s));


        return session.send(messageFlux)
                .and(subscriber)
                .doOnError(ex -> log.error("[{}] - Websocket session error: {}", room, ex.toString(), ex))
                .onErrorResume(ex -> session.close(CloseStatus.SERVER_ERROR))
                .doFinally(s -> log.info("[{}] - Session closed with signal - {} for User ({})", room, s, user));
    }

    private Mono<Void> handleChatMessage(JsonNode json, UUID id, String room, String user,
                                         RTopicReactive topic, ChatMessageCacheTemplateV3 cacheTemplate) {
        String messageText = json.get("message").asText();
        ChatMessage msg = ChatMessage.builder()
                .room(room)
                .userName(user)
                .message(messageText)
                .timestamp(Instant.now())
                .build();

        Mono<ChatMessage> messageMono;

        // insert when id is null, otherwise update
        // When R2DBC / Spring Data tries to update a row in PostgreSQL but cannot find it
        // Postgres returns “0 rows affected,” and Spring wraps it as TransientDataAccessResourceException
        // Need to check if id is null first before calling update

        if(id == null) {
            messageMono = cacheTemplate.insert(msg);
        } else {
            msg.setId(id);
            messageMono = cacheTemplate.update(msg.getId(), msg);
        }

        return messageMono
                .flatMap(saved -> {
                    try {
                        return topic.publish(jsonMapper.writeValueAsString(saved))
                        .then();
                    } catch (JsonProcessingException e) {
                        return Mono.empty();
                    }
                });
    }

    private Mono<Void> handleLoadHistory(JsonNode json, WebSocketSession session, String room) {
        int page = json.get("page").asInt(PAGE);
        int size = json.get("size").asInt(MAX_HISTORY);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        return repository.findByRoom(room, pageable)
                .flatMap(chatMsg -> {
                    try {
                        return session.send(Mono.just(
                                session.textMessage(jsonMapper.writeValueAsString(chatMsg))
                        ));
                    } catch (JsonProcessingException e) {
                        return Mono.empty();
                    }
                })
                .then();
    }

    private Map<String, String> getQueryParams(WebSocketSession session){
        URI uri = session.getHandshakeInfo().getUri();
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .toSingleValueMap();
    }
}
