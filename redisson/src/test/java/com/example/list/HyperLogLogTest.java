package com.example.list;

import com.example.BaseTest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RHyperLogLogReactive;
import org.redisson.client.codec.LongCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class HyperLogLogTest extends BaseTest {

    /**
     * HyperLogLog is a probabilistic data structure used for approximating
     * the cardinality of a multiset.
     * It is particularly useful for counting unique elements in
     * large datasets with low memory usage.
     * HyperLogLog can provide an approximate count of unique elements
     * with a small error margin.
     * It is often used in scenarios where exact counts are not necessary,
     * such as analytics and big data applications.
     *
     * Set can be used to store unique elements,
     * but it consumes more memory compared to HyperLogLog.
     * When dealing with large datasets, set size can grow significantly,
     * leading to increased memory usage.
     * HyperLogLog, on the other hand, uses a fixed amount of memory
     * regardless of the number of unique elements, making it more efficient
     * for counting unique elements in large datasets.
     */

    @Test // 12.5 kb
    public void count(){
        RHyperLogLogReactive<Long> counter = this.client.getHyperLogLog("user:visits", LongCodec.INSTANCE);

        List<Long> list1 = LongStream.rangeClosed(1, 25000)
                .boxed()
                .collect(Collectors.toList());

        List<Long> list2 = LongStream.rangeClosed(25001, 50000)
                .boxed()
                .collect(Collectors.toList());

        List<Long> list3 = LongStream.rangeClosed(1, 75000)
                .boxed()
                .collect(Collectors.toList());

        List<Long> list4 = LongStream.rangeClosed(50000, 100_000)
                .boxed()
                .collect(Collectors.toList());

        Mono<Void> mono = Flux.just(list1, list2, list3, list4)
                .flatMap(counter::addAll)
                .then();

        StepVerifier.create(mono)
                .verifyComplete();

        counter.count()
                .doOnNext(System.out::println)
                .subscribe();
    }


}
