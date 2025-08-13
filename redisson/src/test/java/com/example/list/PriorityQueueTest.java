package com.example.list;

import com.example.BaseTest;
import com.example.queue.Category;
import com.example.queue.PriorityQueue;
import com.example.queue.UserOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.redisson.api.RScoredSortedSetReactive;
import org.redisson.codec.TypedJsonJacksonCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@Execution(ExecutionMode.CONCURRENT)
public class PriorityQueueTest extends BaseTest {

    private PriorityQueue priorityQueue;

    @BeforeAll
    public void setupQueue(){
        RScoredSortedSetReactive<UserOrder> sortedSet =
                this.client.getScoredSortedSet("user:order:queue", new TypedJsonJacksonCodec(UserOrder.class));
        this.priorityQueue = new PriorityQueue(sortedSet);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void producer(){
        Flux.interval(Duration.ofSeconds(1))
                // calculates a base index for each batch
                // l = 0, 1, 2, 3, ... (every second)
                // then multiplies it by 5 to create a base index for the user orders
                // i = 0, 5, 10, 15, ... (every 5 seconds)
                // this ensures that the user orders are added in batches of 5
                .map(l -> (l.intValue() * 5))
                .doOnNext(i -> {
                    UserOrder u1 = new UserOrder(i + 1, Category.GUEST);
                    UserOrder u2 = new UserOrder(i + 2, Category.STD);
                    UserOrder u3 = new UserOrder(i + 3, Category.PRIME);
                    UserOrder u4 = new UserOrder(i + 4, Category.STD);
                    UserOrder u5 = new UserOrder(i + 5, Category.GUEST);
                    Mono<Void> mono = Flux.just(u1, u2, u3, u4, u5)
                            .flatMap(this.priorityQueue::add)
                            .then();
                    StepVerifier.create(mono)
                            .verifyComplete();
                }).subscribe();
        sleep(5_000);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void consumer(){
        this.priorityQueue.takeItems()
                .delayElements(Duration.ofMillis(1000))
                .doOnNext(System.out::println)
                .subscribe();
        sleep(20_000);
    }

}
