package com.spring.common.configs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class RedisCacheNameResolver implements CacheResolver {

    private final CacheManager cacheManager;
    private final RedisConfigMaps redisConfigMaps;

    /**
     * Since Redis SpEL can be only used on attributes like `key`, `condition`, and `unless`,
     * to dynamically set the cache name, use this custom CacheResolver.
     *
     * It resolves the cache names based on the logical names(Map key - math-fib) defined in the RedisConfigMaps.
     *
     * When using on `@Cacheable`, `@CachePut`, or `@CacheEvict` annotations,
     * set cacheNames = <map key from yml> and provide the cacheResolver = "redisCacheNameResolver".
     */

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {

        // logicalNames -> math-fib, other-cache ...
        Collection<String> logicalNames = context.getOperation().getCacheNames();
        List<Cache> resolvedCaches = new ArrayList<>();

        for (String logicalName : logicalNames) {
            // use the logicalName to get the actual cache name from RedisConfigMaps
            RedisConfigMaps.CacheConfig cfg = redisConfigMaps.getCaches().get(logicalName);
            if (cfg == null) {
                throw new IllegalArgumentException("Unknown cache logical name: " + logicalName);
            }

            // get the actual cache name
            String actualName = cfg.getCacheName();
            Cache cache = cacheManager.getCache(actualName);
            if (cache == null) {
                throw new IllegalStateException("Cache not found for name: " + actualName);
            }

            resolvedCaches.add(cache);
        }

        return resolvedCaches;
    }
}
