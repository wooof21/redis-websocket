package com.example.templates;

import com.fasterxml.jackson.core.JsonProcessingException;
import reactor.core.publisher.Mono;

/**
 * Template Design Pattern for Cache Operations
 */
public abstract class CacheTemplate<KEY, ENTITY> {



    /**
     * Retrieves an entity by its key, first checking the cache,
     * and if not found, fetching from the source.
     */
    public Mono<ENTITY> get(KEY key){
        return getFromCache(key)
                    .switchIfEmpty(
                            getFromSource(key)
                                .flatMap(e -> updateCache(key, e))
                    );
    }

    public Mono<ENTITY> insert(ENTITY entity){
        return insertSource(entity)
                    .flatMap(this::insertCache);
    }

    /**
     * Updates an entity in the source.
     * Can either:
     *  1. Update cache first and then source
     *  2. Update source first and then cache
     *  3. Update source and remove from cache
     */
    public Mono<ENTITY> update(KEY key, ENTITY entity){
        return updateSource(key, entity)
                    .flatMap(e ->
                            updateCache(key, e)
//                            deleteFromCache(key).thenReturn(e)
                    );
    }

    /**
     * Deletes an entity from both the cache and the source.
     * The source is deleted first, followed by the cache.
     */
    public Mono<Boolean> delete(KEY key){
        return deleteFromSource(key)
                    .then(deleteFromCache(key));
    }

    /**
     * Abstract methods to be implemented by subclasses
     * for specific cache and source operations.
     */
    abstract protected Mono<ENTITY> insertSource(ENTITY entity);
    abstract protected Mono<ENTITY> insertCache(ENTITY entity);
    abstract protected Mono<ENTITY> getFromSource(KEY key);
    abstract protected Mono<ENTITY> getFromCache(KEY key);
    abstract protected Mono<ENTITY> updateSource(KEY key, ENTITY entity);
    abstract protected Mono<ENTITY> updateCache(KEY key, ENTITY entity);
    abstract protected Mono<Boolean> deleteFromSource(KEY key);
    abstract protected Mono<Boolean> deleteFromCache(KEY key);

}
