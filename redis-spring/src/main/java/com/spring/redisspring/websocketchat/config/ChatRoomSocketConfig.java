package com.spring.redisspring.websocketchat.config;

import com.spring.redisspring.websocketchat.service.ChatRoomServiceV1;
import com.spring.redisspring.websocketchat.service.ChatRoomServiceV2;
import com.spring.redisspring.websocketchat.service.ChatRoomServiceV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.Map;

@Configuration
public class ChatRoomSocketConfig {

//    @Autowired
//    private ChatRoomServiceV1 chatRoomService;

//    @Autowired
//    private ChatRoomServiceV2 chatRoomService;

    @Autowired
    private ChatRoomServiceV3 chatRoomService;

    /**
     * Spring WebFlux has multiple HandlerMapping beans
     *  - RequestMappingHandlerMapping (for @Controller / @RestController)
     *  - RouterFunctionMapping (for functional routes, if used)
     *  - ResourceHandlerMapping (for static resources like /static/**)
     *  - SimpleUrlHandlerMapping (like your WebSocket mapping)
     *
     * All Beans have the same priority by default - 0
     *  - the lower the number, the higher the priority
     *  - so set -1 to give this mapping a higher priority than the others
     *      - this tells Spring to first check this mapping before all the other mappings
     */
    @Bean
    public HandlerMapping handlerMapping(){
        Map<String, WebSocketHandler> map = Map.of(
                // /chat?room=<room_name>&user=<user_name>
                "/chat", chatRoomService
        );
        return new SimpleUrlHandlerMapping(map, -1);
    }

}
