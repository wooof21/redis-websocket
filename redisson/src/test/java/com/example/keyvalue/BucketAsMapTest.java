package com.example.keyvalue;

import com.example.BaseTest;
import org.junit.jupiter.api.Test;
import org.redisson.client.codec.StringCodec;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

public class BucketAsMapTest extends BaseTest {
    // user:1:name
    // user:2:name
    // user:3:name
    @Test
    public void bucketsAsMap(){
        Map<String, String> users = Map.of(
                "user:1:name", "john",
                "user:2:name", "jane",
                "user:3:name", "mark"
        );

        Mono<Void> setAll = Flux.fromIterable(users.entrySet())
                .flatMap(entry ->
                        this.client.getBucket(entry.getKey(), StringCodec.INSTANCE)
                                .set(entry.getValue()))
                .then();

        Mono<Map<String, Object>> getAll = this.client.getBuckets(StringCodec.INSTANCE)
                // .get("user:1:name", "user:2:name", "user:3:name")
                .get(users.keySet().toArray(new String[0]))
                .doOnNext(System.out::println);

        StepVerifier.create(setAll.then(getAll))
                .expectNextMatches(map -> {
                    // Check if the map contains all the expected keys and values
                    return map.size() == users.size() &&
                            ObjectUtils.nullSafeEquals("john", map.get("user:1:name")) &&
                            ObjectUtils.nullSafeEquals("jane", map.get("user:2:name")) &&
                            ObjectUtils.nullSafeEquals("mark", map.get("user:3:name"));
                })
                .verifyComplete();


    }

}
