package com.spring.redisspring.websocketchat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.redisspring.websocketchat.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RListReactive;
import org.redisson.api.RTopicReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static com.spring.redisspring.websocketchat.config.Constants.*;

/**
 * Spring WebFlux WebSocket Chat Service backed by Redis via Redisson:
 *  - Redis: for message persistence and pub/sub
 *  - org.springframework.web.reactive.socket.WebSocketHandler:
 *      - Handles WebSocket connections in a reactive way
 */
//@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceV1 implements WebSocketHandler {


    private final RedissonReactiveClient client;
    private final ObjectMapper jsonMapper; // Spring Boot auto-configures one

    /**
     * Handles WebSocket connection:
     *  - get chat room name from query param: ws://.../chat?room=<room_name>
     *  - RTopicReactive: Redis pub/sub topic for live messaging
     *  - RListReactive: Redis list for storing chat history: key -> (history:room)
     *
     * Subscriber:
     *  - webSocketSession.receive(): Stream of messages sent by client
     *  - Convert to text → store in Redis list → publish on Redis topic
     *  - subscribe(): starts the reactive stream and keep streaming
     *
     * Publisher:
     *  - Subscribes to Redis topic → receives new chat messages
     *  - .startWith(list.iterator()): Before listening live, send all past history to the new client
     *  - Converts each message into a WebSocketMessage
     *
     * **************************************************
     *
     * Backpressure: tell the producer to slow down so the consumer isn’t overwhelmed,
     * In WebFlux: controlling demand so that data is pushed at the rate a downstream subscriber can handle
     *  - without: Subscriber
     *      - Client could flood messages (spamming fast)
     *      - flatMap → processes them in parallel, unbounded by default
     *      - f Redis (or network) is slower than incoming rate → risk of memory buildup/OOM
     *  - without: Publisher
     *      - Redis pub/sub can deliver bursts of messages
     *      - If client’s WebSocket connection is slow ->
     *          messages accumulate in memory, server gets overloaded
     *  - with: Subscriber
     *      - Use limitRate, onBackpressureDrop, or concatMap
     *
     */
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Map<String, String> queryParams = getQueryParams(session);
        String room = queryParams.getOrDefault("room", "default");
        String user = queryParams.getOrDefault("user", "anonymous");

        RTopicReactive topic = this.client.getTopic(room, StringCodec.INSTANCE);
        RListReactive<String> historyCache = this.client.getList("history:" + room, StringCodec.INSTANCE);

        // Subscribe - receive messages from a client
        Mono<Void> subscribes = session.receive()
                .takeUntilOther(session.closeStatus()) // complete when clients closed
                .map(WebSocketMessage::getPayloadAsText)
                .onBackpressureDrop(msg ->
                        log.warn("[{}] - Dropped incoming msg for user - {} due to backpressure", room, user))
                .map(msg -> jsonChatMessage(user, msg))
                /**
                 * flatMap(..., MAX_IN_FLIGHT_WRITES):
                 *  - Processes messages concurrently, up to the limit of MAX_IN_FLIGHT_WRITES
                 *  - Message Order not guaranteed
                 *  - But:
                 *      - Higher throughput since multiple writes/publishes can run in parallel
                 *      - Backpressure friendly: once there are MAX_IN_FLIGHT_WRITES running, new messages wait.
                 *      - Use when care more about speed & scalability than strict ordering
                 * Use concatMap(...) for strick order:
                 *  - Processes one message at a time in strict order (FIFO).
                 *  - Each .add() and .publish() must finish before the next message starts.
                 *  - Guarantees message ordering (the order messages arrive from the WebSocket is
                 *      the order they’re written to Redis & published).
                 *  - But:
                 *      - throughput can be low if Redis operations are slow or if messages come in
                 *      - Use when Ordering MUST maintain
                 *
                 */
                .flatMap(chatMsg ->
                        historyCache.add(chatMsg)
                                // maintain history size
                                .then(trimHistoryIfNeeded(historyCache))
                                // publish to room
                                .then(topic.publish(chatMsg)), MAX_IN_FLIGHT_WRITES)

                /**
                 * .doOnError(System.out::println):
                 *  - just logs but doesn’t recover → the stream terminates
                 *  - If Redis is temporarily unavailable or a client sends malformed JSON,
                 *      the session could just crash without notice.
                 *  Replace with .onErrorResume
                 */
                .then() // convert Flux<Long> to Mono<Void>
                .onErrorResume(ex -> {
                    log.error("[{}] - Error processing inbound message: {}", room, ex.toString(), ex);
                    // WebSocketMessage err = session.textMessage(jsonChatMessage(user, "Inbound error: " + ex.getMessage()));
                    // return session.send(Mono.just(err)) // send error message to client
                    //        .then(session.close(CloseStatus.SERVER_ERROR)); // close session with error status
                    return Mono.empty(); // continue processing future messages
                })
                .doOnSubscribe(s -> log.info("User ({}) joined room - [{}]", user, room))
                .doFinally(s -> log.info("Subscriber Finally: {} ", s));
                /**
                 * .subscribe(): subscribe and start streaming - its not tied to WebSocketSession lifecycle
                 *      - Even if the client disconnects, that subscription may keep running:
                 *           potential memory leak, dangling Redis ops, or wasted processing.
                 *
                 * Instead of running forever: tie it to the WebSocket lifecycle
                 *   - WebSocketHandler.handle(...) expects to return a `Mono<Void>` that
                 *       represents the entire lifetime of the session.
                 *       - When that Mono completes, Spring closes the WebSocket
                 *       - If subscribe() separately, Spring doesn’t “know” about that work
                 *   - Instead of .subscribe(), merge the streams
                 */
//                .subscribe();

        // Publisher
        Flux<WebSocketMessage> publishes = topic.getMessages(String.class)
                .startWith(historyCache.iterator())
                // Backpressure on sending
                .onBackpressureBuffer(
                        SEND_BUFFER,
                        dropped -> log.warn("Dropping oldest messages for room '{}' - Dropped: {}", room, dropped),
                        // drop oldest when buffer full (real-time > completeness)
                        BufferOverflowStrategy.DROP_OLDEST
                )
                .map(session::textMessage)
                .doOnSubscribe(s -> log.info("[{}] - Publisher registered for User ({})", room, user))
                .doFinally(s -> log.info("Publisher Finally: {} ", s));

        // Push messages to clients
//        return webSocketSession.send(flux);

        // Chain: session send + session receive
        return session
                .send(publishes)
                .and(subscribes)
                // top-level error handler for the session
                .doOnError(e -> log.error("[{}] - Websocket session error: {}", room, e.toString(), e))
                // When error happens on server side, close the session with error status
                .onErrorResume(e -> session.close(CloseStatus.SERVER_ERROR))
                .doFinally(s -> log.info("[{}] - Session closed with signal - {} for User ({})", room, s, user));
    }

    private Map<String, String> getQueryParams(WebSocketSession session){
        URI uri = session.getHandshakeInfo().getUri();
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .toSingleValueMap();
    }

    private String jsonChatMessage(String user, String msg) {
        ChatMessage chatMessage = ChatMessage.builder()
                .userName(user)
                .message(msg)
                .timestamp(Instant.now())
                .build();
        try {
            return jsonMapper.writeValueAsString(chatMessage);
        } catch (JsonProcessingException e) {
            // return toString() plain text
            return chatMessage.toString();
        }
    }

    // Remove oldest entries until size <= MAX_HISTORY
    private Mono<Void> trimHistoryIfNeeded(RListReactive<String> history) {
        // querying size each time is O(1) in Redis; removing index [0,n] is O(n) — OK for small MAX_HISTORY
        return history.size()
                .flatMap(size -> {
                    if (size <= MAX_HISTORY) return Mono.empty();
                    // Redis List LTRIM
                    return history.trim(size - MAX_HISTORY, -1); // keep last MAX_HISTORY items
                });
    }


}
