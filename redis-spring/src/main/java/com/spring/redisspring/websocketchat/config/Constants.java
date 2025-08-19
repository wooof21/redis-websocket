package com.spring.redisspring.websocketchat.config;

import reactor.util.concurrent.Queues;

public class Constants {


    // cache latest N messages per room in Redis,
    // all later new joined users will only receive the last 100 messages from Redis
    public static final int MAX_HISTORY = 10;
    public static final int SEND_BUFFER = Queues.SMALL_BUFFER_SIZE;        // outgoing buffer size (protect memory)
    public static final int MAX_IN_FLIGHT_WRITES = 16; // Redis publish/list add concurrency
    public static final int PAGE = 1;               // default page number for pagination
}
