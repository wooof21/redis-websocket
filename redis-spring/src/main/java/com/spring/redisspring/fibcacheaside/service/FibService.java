package com.spring.redisspring.fibcacheaside.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.spring.common.CacheNameConstants.FIB_CACHE_KEY;

@Service
@Slf4j
public class FibService {


    /**
     *  cacheNames/value defines the name of the cache where the results will be stored.
     *  key defines the key(ke for each Hash item) under which the result will be cached.
     *
     *  When `key` is not specified, the default key generation strategy is used,
     *  which typically uses the method parameters to create a unique key.
     *  Means if there are multiple parameters, Eg. int index, String name,
     *  then the key will be a combination of both.
     *
     *  Cacheable properties use a constant or a SpEL expression
     *  that resolves to a string at runtime to set the value
     */
    @Cacheable(cacheNames = FIB_CACHE_KEY, cacheResolver = "redisCacheNameResolver", key = "#index")
    public int getFib(int index, String name) {
        log.info("Calculating Fibonacci for name: {} with index: {}", name, index);
        return this.fib(index);
    }

    /**
     * Cache Eviction Strategy, evicts the cache entry for the given index.
     *
     * Usually used when PUT, POST, PATCH, DELETE
     */
    @CacheEvict(cacheNames = FIB_CACHE_KEY, cacheResolver = "redisCacheNameResolver", key = "#index")
    public void evictByKey(int index) {
        log.info("Remove Hash entry for index: {}", index);
    }

    // Run a CRON job to clear the entire cache periodically
  //  @Scheduled(fixedRate = 10_000)
    @CacheEvict(cacheNames = FIB_CACHE_KEY, cacheResolver = "redisCacheNameResolver", allEntries = true)
    public void clearCache() {
        log.info("Flushing Cache for all entries.");
    }

    //Time: O(2^n)
    private int fib(int index){
        if(index < 2) return index;
        return fib(index - 1) + fib(index - 2);
    }

}
