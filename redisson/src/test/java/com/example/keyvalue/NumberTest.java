package com.example.keyvalue;

import com.example.BaseTest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLongReactive;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

public class NumberTest extends BaseTest {

    @Test
    public void keyValueIncreaseTest(){
        // set k v -- incr , decr
        // to store a number in Redis, use RAtomicLongReactive
        // available for incr and decr operations
        RAtomicLongReactive atomicLong =
                this.client.getAtomicLong("user:1:visit");
        //emit a number from 1 to 5 every second
        Mono<Long> incr = Flux.range(1, 5)
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(System.out::println)
                // for each emitted number, increment the atomic long
                .flatMap(i -> atomicLong.incrementAndGet())
                .count();
        StepVerifier.create(incr)
                // expect the count of emitted numbers to be 5
                .expectNext(5L)
                .verifyComplete();
    }

}
