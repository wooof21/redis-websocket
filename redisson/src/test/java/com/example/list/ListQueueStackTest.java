package com.example.list;

import com.example.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RDequeReactive;
import org.redisson.api.RListReactive;
import org.redisson.api.RQueueReactive;
import org.redisson.client.codec.LongCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class ListQueueStackTest extends BaseTest {

    @Test
    public void listTest(){
        // lrange number-input 0 -1
        RListReactive<Long> list = this.client.getList("number-list", LongCodec.INSTANCE);

        /**
         * Mono<Void> listAdd = Flux.range(1, 10)
         *                 .map(Long::valueOf)
         *                 .flatMap(list::add)
         *                 .then();
         * By this way, the list item may not be added in order.
         * Since Flux is asynchronous, and it creates multiple threads to process the items.
         * So, the items may be added in different order.
         * To ensure the order, use a stream to collect the items and then add
         * the items to the list in one go.
         */

        List<Long> longList = LongStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());

        StepVerifier.create(list.addAll(longList).then())
                .verifyComplete();
        StepVerifier.create(list.size())
                .expectNext(10)
                .verifyComplete();
        // Verify element at index 5 is 6L
        StepVerifier.create(list.get(5))
                .expectNext(6L)
                .verifyComplete();
    }

    @Test
    public void queueTest(){
        RQueueReactive<Long> queue = this.client.getQueue("number-queue", LongCodec.INSTANCE);
        List<Long> longList = LongStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());

        StepVerifier.create(queue.addAll(longList).then())
                .verifyComplete();

        Mono<Void> queuePoll = queue.poll()
                //queue poll 4 times
                .repeat(3)
                .doOnNext(System.out::println)
                .then();
        StepVerifier.create(queuePoll)
                .verifyComplete();
        StepVerifier.create(queue.size())
                .expectNext(6)
                .verifyComplete();
    }

    // Stack is deprecated in Redisson, use Deque instead
    // Deque is a double-ended queue, which can be used as a stack
    // Stack is a LIFO (Last In First Out) data structure, which means the last item added is the first one to be removed.
    // Deque can be used as a stack by using the pollLast() method to remove the last item added.
    @Test
    public void stackTest(){ // Deque
        RDequeReactive<Long> deque = this.client.getDeque("stack-input", LongCodec.INSTANCE);

        List<Long> longList = LongStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());

        StepVerifier.create(deque.addAll(longList).then())
                .verifyComplete();

        // pollLast() is used to remove the last item added to the stack
        // pollFirst() is used to remove the first item added to the queue - same as queue.poll()
        Mono<Void> stackPoll = deque.pollLast()
                .repeat(3)
                .doOnNext(System.out::println)
                .then();
        StepVerifier.create(stackPoll)
                .verifyComplete();
        StepVerifier.create(deque.size())
                .expectNext(6)
                .verifyComplete();
    }


}
