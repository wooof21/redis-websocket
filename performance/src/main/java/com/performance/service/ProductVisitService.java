package com.performance.service;

import org.redisson.api.BatchOptions;
import org.redisson.api.RBatchReactive;
import org.redisson.api.RScoredSortedSetReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Product visit count service:
 *
 *  - Collect incoming product visit events (product IDs).
 *  - Batch them every 10 seconds.
 *  - Aggregate counts per product ID.
 *  - Update Redis in a single Redisson reactive batch.
 *
 * Sink with backpressure buffer avoids data loss under high load.
 *
 * Type:
 *  - Many (Sinks.Many<T>): Used when expect multiple elements over time (like an event stream).
 *    - unicast() → only one subscriber allowed, keeps order.
 *    - multicast() → broadcasts to many subscribers, but only sends new events after subscription.
 *    - replay() → broadcasts to many subscribers and can replay past events to new subscribers.
 *  - One (Sinks.One<T>): Emits exactly one value (or an error) to a single subscriber.
 *  - Empty (Sinks.Empty<T>): Just emits a completion or error signal (no data).
 * Emit:
 *  - emitNext() → strict, throws if it can’t emit (e.g., sink closed).
 *  - tryEmitNext() → returns a result (EmitResult) instead of throwing.
 */
@Service
public class ProductVisitService {

    private final RedissonReactiveClient client;
    // Reactive sink to push product IDs into a reactive pipeline.
    private final Sinks.Many<Integer> sink;

    public ProductVisitService(RedissonReactiveClient client) {
        this.client = client;
        // Only one subscriber and If events come faster than they can be processed,
        // they’re buffered in memory.
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
    }

    @PostConstruct
    private void init(){
        // Converts sink into a reactive stream (asFlux()).
        this.sink
                .asFlux()
                // Every 10 seconds, collect all product IDs emitted in that time
                // window into a List<Integer>.
                .buffer(Duration.ofSeconds(10)) // list of productIds: (1,2,1,1,3,5,1...)
                // Convert the list into a Map<Integer, Long> where:
                // Key = product ID, Value = number of visits in that batch.
                .map(l -> l.stream().collect(    // 1:4, 2:1, 3:1, 5:1,
                        Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()
                        )
                ))
                // Pass aggregated counts to updateBatch.
                .flatMap(this::updateBatch)
                // Subscribe to start the flow.
                .subscribe();
    }

    // Pushes a product ID into the reactive sink.
    // non-blocking and just enqueues for later batch processing.
    public void addVisit(int productId){
        this.sink.tryEmitNext(productId);
    }

    private Mono<Void> updateBatch(Map<Integer, Long> map){
        // Groups multiple Redis commands into a single network round-trip.
        RBatchReactive batch = this.client.createBatch(BatchOptions.defaults());
        // Key name → "product:visit:YYYYMMdd" → Each day has its own sorted set.
        String format = DateTimeFormatter.ofPattern("YYYYMMdd").format(LocalDate.now());
        // Redis sorted set where:
        // Member = product ID - Score = visit count.
        RScoredSortedSetReactive<Integer> set =
                batch.getScoredSortedSet("product:visit:" + format, IntegerCodec.INSTANCE);

        // For each (productId, count) in the map:
        return Flux.fromIterable(map.entrySet())
                // set.addScore(productId, count) increments score.
                .map(entry -> set.addScore(entry.getKey(), entry.getValue()))
                // batch.execute() sends all increments at once to Redis.
                .then(batch.execute())
                .then();
    }

}
