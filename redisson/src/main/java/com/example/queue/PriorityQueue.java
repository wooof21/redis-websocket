package com.example.queue;

import org.redisson.api.RScoredSortedSetReactive;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;

public class PriorityQueue {
    
    private final RScoredSortedSetReactive<UserOrder> queue;

    public PriorityQueue(RScoredSortedSetReactive<UserOrder> queue) {
        this.queue = queue;
    }

    public Mono<Void> add(UserOrder userOrder){
        return this.queue.add(
                getScore(userOrder.getCategory()),
                userOrder
        ).then();
    }

    public Flux<UserOrder> takeItems(){
        // Take items from the queue based on their score
        // The items with the highest score will be taken first
        // The queue is sorted in descending order by score
        // and the items are taken in batches of 1
        // to ensure that the consumer processes them one by one
        // This allows for a fair distribution of items based on their category
        // and ensures that the consumer does not get overwhelmed or loose items
        // when crashing or restarting
        return this.queue.takeFirstElements()
                         .limitRate(1);
    }

    // score is calculated based on the category and the current timestamp
    // ensure earlier added item with the same category has a lower score
    // so that it will be processed first
    private Long getScore(Category category){
        return category.getScore() + Instant.now().getEpochSecond();
    }

}
