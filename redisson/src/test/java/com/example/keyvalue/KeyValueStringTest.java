package com.example.keyvalue;

import com.example.BaseTest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucketReactive;
import org.redisson.client.codec.StringCodec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

public class KeyValueStringTest extends BaseTest {

    /**
     *  Accessing a key-value pair in Redis using Redisson Reactive API
     *  RBucketReactive: a reactive interface for Redis bucket operations
     *  StringCodec: used to encode/decode the string values
     *  Mono: a Reactor type that represents a single asynchronous value
     *  StepVerifier: used to test the reactive stream
     *
     *  The test sets a value "sam" for the key "user:1:name"
     *  and then retrieves it, printing the value to the console.
     */
    @Test
    public void keyValueAccessTest(){
        RBucketReactive<String> bucket = this.client.getBucket("user:1:name", StringCodec.INSTANCE);
        Mono<Void> set = bucket.set("john");
        Mono<String> get = bucket.get()
                .doOnNext(System.out::println);
        //do set first and then get
        StepVerifier.create(set.then(get))
                .expectNext("john")
                .verifyComplete();
    }

    @Test
    public void keyValueExpiryTest() throws InterruptedException {
        RBucketReactive<String> bucket = this.client.getBucket("user:1:name", StringCodec.INSTANCE);
        Mono<Void> set = bucket.set("john", Duration.ofSeconds(2));
        Mono<String> get = bucket.get()
                .doOnNext(System.out::println);
        StepVerifier.create(set.then(get))
                .expectNext("john")
                .verifyComplete();
        // wait for 6 seconds to let the key expire
        sleep(3000);
        StepVerifier.create(get)
                //0 elements emitted at the end
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    public void keyValueExtendExpiryTest(){
        RBucketReactive<String> bucket = this.client.getBucket("user:1:name", StringCodec.INSTANCE);
        Mono<Void> set = bucket.set("john", Duration.ofSeconds(5));
        Mono<String> get = bucket.get()
                .doOnNext(System.out::println);
        StepVerifier.create(set.then(get))
                .expectNext("john")
                .verifyComplete();
        //extend
        sleep(2000);
        //set new expiration time to bucket
        Mono<Boolean> mono = bucket.expire(Duration.ofSeconds(10));
        StepVerifier.create(mono)
                .expectNext(true)
                .verifyComplete();
        // access expiration time
        Mono<Void> ttl = bucket.remainTimeToLive()
                .doOnNext(System.out::println)
                .then();
        StepVerifier.create(ttl)
                .verifyComplete();
    }


}
