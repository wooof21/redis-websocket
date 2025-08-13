package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;

import java.util.Objects;

@Configuration
public class RedissonConfig {

    private RedissonClient client;

    @Bean
    public RedissonClient getClient(){
        if(Objects.isNull(this.client)){
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379");
            client = Redisson.create(config);
        }
        return client;
    }

    @Bean
    public RedissonReactiveClient getRedissonReactiveClient(){
        return getClient().reactive();
    }
}
