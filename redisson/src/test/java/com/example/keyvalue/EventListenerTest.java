package com.example.keyvalue;

import com.example.BaseTest;
import org.junit.jupiter.api.Test;
import org.redisson.api.DeletedObjectListener;
import org.redisson.api.ExpiredObjectListener;
import org.redisson.api.RBucketReactive;
import org.redisson.client.codec.StringCodec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

public class EventListenerTest extends BaseTest {

    @Test
    public void expiredEventTest(){
        RBucketReactive<String> bucket = this.client.getBucket("user:1:name", StringCodec.INSTANCE);
        Mono<Void> set = bucket.set("john", Duration.ofSeconds(2));
        Mono<String> get = bucket.get()
                .doOnNext(System.out::println);

        //by default, Redis will not notify about expired keys
        //we need to enable keyspace notifications in redis.conf
        //Enable keyspace notifications in Redis
        //Add this to the redis.conf or run at startup(redis-cli):
        //CONFIG SET notify-keyspace-events Ex
        //https://redis.io/docs/latest/develop/pubsub/keyspace-notifications/#configuration
        Mono<Void> event = bucket.addListener(
                (ExpiredObjectListener) key ->
                        System.out.println("Expired : " + key)).then();

        StepVerifier.create(set.then(get).then(event))
                .verifyComplete();
        //extend
        sleep(3000);
    }

    @Test
    public void deletedEventTest(){
        RBucketReactive<String> bucket = this.client.getBucket("user:1:name", StringCodec.INSTANCE);
        Mono<Void> set = bucket.set("john");
        Mono<String> get = bucket.get()
                .doOnNext(System.out::println);

        Mono<Boolean> delete = bucket.delete();

        //delete key from redis-cli to trigger the event
        Mono<Void> event = bucket.addListener(
                (DeletedObjectListener) key ->
                        System.out.println("Deleted : " + key)).then();

        StepVerifier.create(set
                        .then(get)
                        .then(event)
                        .then(Mono.delay(Duration.ofSeconds(2)))
                        .then(delete))
                .expectNext(true)
                .verifyComplete();
    }


}
