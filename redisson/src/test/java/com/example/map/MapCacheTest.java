package com.example.map;

import com.example.BaseTest;
import com.example.model.Student;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMapCacheReactive;
import org.redisson.codec.TypedJsonJacksonCodec;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MapCacheTest extends BaseTest {

    /**
     * Redis Hash - only allow expiring the entire hash(map), not individual field
     * Redisson Map - allows expiring the individual fields in the map
     * RMapCache - allows expiring individual keys in the map
     * RMapCache is a combination of RMap and RBucket
     * RMapCache is a distributed map with TTL support for individual keys
     * RMapCacheReactive is a reactive map with TTL support for individual keys
     */
    @Test
    public void mapCacheTest(){
        // Map<Integer, Student>
        TypedJsonJacksonCodec codec = new TypedJsonJacksonCodec(Integer.class, Student.class);
        RMapCacheReactive<Integer, Student> mapCache = this.client.getMapCache("users:cache", codec);

        Map<Integer, Student> javaMap = Map.of(
                1, Student.builder().name("jane").age(37).city("Calgary").build(),
                2, Student.builder().name("mike").age(88).city("Woodbridge").build()
        );

        Mono<Void> putAll = Mono.when(
                mapCache.put(1, javaMap.get(1), 2, TimeUnit.SECONDS),
                mapCache.put(2, javaMap.get(2), 5, TimeUnit.SECONDS)
        );

        StepVerifier.create(putAll.thenMany(
                        Mono.zip(mapCache.get(1),mapCache.get(2))
                ))
                .expectNextMatches(tuple -> {
                    Student student1 = tuple.getT1();
                    Student student2 = tuple.getT2();
                    return student1.getName().equals("jane") &&
                            student1.getAge() == 37 &&
                            student1.getCity().equals("Calgary") &&
                            student2.getName().equals("mike") &&
                            student2.getAge() == 88 &&
                            student2.getCity().equals("Woodbridge");
                })
                .verifyComplete();


        Mono<Student> get1 = mapCache.get(1)
                .doOnNext(student -> System.out.println("Student 1: " + student));
        Mono<Student> get2 = mapCache.get(2)
                .doOnNext(student -> System.out.println("Student 2: " + student));

        sleep(3000);


        StepVerifier.create(get1.then(get2))
                .expectNextMatches(student -> {
                    // student 1 should be expired
                    return "mike".equals(student.getName());
                })
                .verifyComplete();

        // sleep for 3 more seconds to ensure student 2 is also expired
        sleep(3000);
        StepVerifier.create(get2)
                // student 2 should be expired
                // Publisher emits zero items(i.e., no onNext signal is received) before completing.
                // mapCache.get(2) returns an empty Mono (completes without emitting any value)
                .expectNextCount(0)
                .verifyComplete();


    }

}
