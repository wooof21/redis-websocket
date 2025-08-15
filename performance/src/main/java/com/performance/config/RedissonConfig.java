package com.performance.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Objects;

@Configuration
public class RedissonConfig {

    private RedissonClient client;

    /**
     * Spring’s Lettuce pool config is only for Spring Data Redis.
     * Redisson has its own pool settings via Config (YAML or programmatic).
     */
    @Bean
    public RedissonClient getClient(){
        if(Objects.isNull(this.client)){
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379")
                    .setConnectionPoolSize(200) // max connections per node (increase if heavy parallelism)
                    .setConnectionMinimumIdleSize(20) //keep these warm to avoid reconnect churn
                    .setIdleConnectionTimeout(10000) //ms before closing idle connection
                    .setConnectTimeout(5000) //ms to establish TCP connection
                    .setTimeout(5000) //ms to wait for Redis command
                    .setRetryAttempts(3) //retry Redis command on failure
//                    .setKeepAlive(true) //enable TCP keep-alive
                    .setTcpNoDelay(true); //disable Nagle's algorithm for low latency
            // Redisson threads for IO (CPU cores × 2 is good)
            config.setThreads(8);
            // Netty event loop threads
            config.setNettyThreads(16);
            client = Redisson.create(config);
        }
        return client;
    }

    @Bean
    public RedissonReactiveClient getRedissonReactiveClient(){
        return getClient().reactive();
    }
}
