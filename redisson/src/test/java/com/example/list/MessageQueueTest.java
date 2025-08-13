package com.example.list;

import com.example.BaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.redisson.api.RBlockingDequeReactive;
import org.redisson.client.codec.LongCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@Execution(ExecutionMode.CONCURRENT)
public class MessageQueueTest extends BaseTest {

    private RBlockingDequeReactive<Long> msgQueue;

    @BeforeAll
    public void setupQueue(){
        // BlockingDeque is used for message queue - Java naming convention
        // It is not a blocking queue, but a reactive blocking deque
        // It allows multiple consumers to consume messages concurrently
        // and ensures that messages are processed in the order they are added.
        this.msgQueue = this.client.getBlockingDeque("message-queue", LongCodec.INSTANCE);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void producer(){
        Mono<Void> mono = Flux.range(1, 35)
                .delayElements(Duration.ofMillis(500))
                .doOnNext(i -> System.out.println("Producer publish: " + i))
                .flatMap(i -> this.msgQueue.add(Long.valueOf(i)))
                .then();
        StepVerifier.create(mono)
                .verifyComplete();
    }


    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void consumer1(){
        this.msgQueue.takeElements()
                .doOnNext(i -> System.out.println("Consumer 1 : " + i))
                .doOnError(System.out::println)
                .subscribe();
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void consumer2(){
        this.msgQueue.takeElements()
                .doOnNext(i -> System.out.println("Consumer 2 : " + i))
                .doOnError(System.out::println)
                .subscribe();
    }
}
