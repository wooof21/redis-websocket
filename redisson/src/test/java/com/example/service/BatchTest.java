package com.example.service;

import com.example.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatchReactive;
import org.redisson.api.RListReactive;
import org.redisson.api.RSetReactive;
import org.redisson.client.codec.LongCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

public class BatchTest extends BaseTest {

    /**
     * Use batch operations for better performance
     * when dealing with a large number of operations.
     * Batch operations allow to group multiple commands into a single request,
     * reducing the number of round trips to the server.
     * This is particularly useful when need to perform
     * a large number of operations, such as adding a large number of elements to a list
     * or a set, as shown in this example.
     */
    @Test // 1.5 seconds
    public void batchTest(){
        RBatchReactive batch = this.client.createBatch(BatchOptions.defaults());
        RListReactive<Long> list = batch.getList("numbers-list", LongCodec.INSTANCE);
        RSetReactive<Long> set = batch.getSet("numbers-set", LongCodec.INSTANCE);
        for (long i = 0; i < 50_000; i++) {
            list.add(i);
            set.add(i);
        }
        StepVerifier.create(batch.execute().then())
                .verifyComplete();
    }

    @Test // 4 seconds
    public void regularTest(){
        RListReactive<Long> list = this.client.getList("numbers-list", LongCodec.INSTANCE);
        RSetReactive<Long> set = this.client.getSet("numbers-set", LongCodec.INSTANCE);
        Mono<Void> mono = Flux.range(1, 50_000)
                .map(Long::valueOf)
                .flatMap(i -> list.add(i).then(set.add(i)))
                .then();
        StepVerifier.create(mono)
                .verifyComplete();
    }

}
