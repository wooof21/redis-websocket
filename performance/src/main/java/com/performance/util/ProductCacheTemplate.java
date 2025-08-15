package com.performance.util;

import com.performance.model.Product;
import com.performance.repository.ProductRepository;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class ProductCacheTemplate extends CacheTemplate<Integer, Product> {

    private final ProductRepository repository;

    private final RMapReactive<Integer, Product> mapCache;

    public ProductCacheTemplate(ProductRepository repository,
                                RedissonReactiveClient client) {
        this.repository = repository;
        this.mapCache = client.getMap("product",
                new TypedJsonJacksonCodec(Integer.class, Product.class));
    }

    @Override
    protected Mono<Product> getFromSource(Integer id) {
        return this.repository.findById(id)
//                .delaySubscription(Duration.ofMillis(30))
                ;
    }

    @Override
    protected Mono<Product> getFromCache(Integer id) {
        return this.mapCache.get(id)
                .onErrorResume(e -> Mono.empty()); // Ignore Redis errors
    }

    @Override
    protected Mono<Product> updateSource(Integer id, Product product) {
        return this.repository.save(product);
    }

    @Override
    protected Mono<Product> updateCache(Integer id, Product product) {
        return this.mapCache.fastPut(id, product)
                .onErrorResume(e -> Mono.empty())
                .thenReturn(product);
    }

    @Override
    protected Mono<Boolean> deleteFromSource(Integer id) {
        return this.repository.deleteById(id)
                .thenReturn(true)
                // Handle case where product does not exist
                .onErrorResume(e -> Mono.just(false));
    }

    @Override
    protected Mono<Boolean> deleteFromCache(Integer id) {
        return this.mapCache.fastRemove(id)
                .onErrorResume(ex -> Mono.just(0L)) // Ignore Redis errors
                .flatMap(result ->
                        result > 0 ? Mono.just(true) :
                                Mono.just(false));

    }

}
