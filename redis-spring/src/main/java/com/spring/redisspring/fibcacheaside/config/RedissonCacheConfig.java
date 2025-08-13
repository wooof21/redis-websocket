package com.spring.redisspring.fibcacheaside.config;

import com.spring.common.configs.RedisConfigMaps;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
public class RedissonCacheConfig {

    private RedissonClient client;

    @Autowired
    private RedisConfigMaps redisConfigMaps;

    /**
     * CacheManager is a Spring interface that manages cache instances.
     * It provides access to named caches and handles storing, retrieving,
     * and evicting cached data.
     *
     * In Spring Boot, it integrates with the @Cacheable, @CachePut, and @CacheEvict
     * annotations to enable transparent caching for methods and objects.
     *
     * Different implementations (like RedissonSpringCacheManager) connect to various
     * cache providers (Redis, EhCache, etc.).
     *
     * By default, Spring Boot uses a simple in-memory cache manager if not configured,
     * using ConcurrentHashMap.
     *
     * `configMap` is for cache-specific settings (TTL, max idle) used by RedissonSpringCacheManager.
     */
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient){
        Map<String, CacheConfig> configMap = new HashMap<>();
        redisConfigMaps.getCaches().values().forEach(c -> {
            configMap.put(c.getCacheName(),
                    new CacheConfig(c.getTtl(), 0));
        });

        return new RedissonSpringCacheManager(redissonClient, configMap);
//        return new RedissonSpringCacheManager(redissonClient);
    }

    // `Config` is for Redisson client connection settings (Redis address, codec, etc.).
    @Bean
    public RedissonClient getClient(){
        if(Objects.isNull(this.client)){
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379");
            config.setCodec(new JsonJacksonCodec()); // JSON serialization for values

            client = Redisson.create(config);
        }
        return client;
    }

    @Bean
    public RedissonReactiveClient getRedissonReactiveClient(){
        return getClient().reactive();
    }
}
