package com.spring.common.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "redis")
@Data
public class RedisConfigMaps {

    private Map<String, CacheConfig> caches;

    @Data
    public static class CacheConfig {
        private String cacheName;
        private long ttl;
    }

}
