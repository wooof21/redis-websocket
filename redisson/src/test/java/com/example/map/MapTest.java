package com.example.map;

import com.example.BaseTest;
import com.example.model.Student;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMapReactive;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.TypedJsonJacksonCodec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

//Redis Hash -> Redisson Map
public class MapTest extends BaseTest {

    @Test
    public void mapCreateTest(){
        RMapReactive<String, String> map = this.client.getMap("user:1", StringCodec.INSTANCE);
        //RMapReactive.put returns a Mono emitting the previous value
        // for the key (or null if none), not the value just put.
        // Since the map is empty, each put emits null.
        // or use get to retrieve the current value after putting.
//        Mono<String> name = map.put("name", "john");
//        System.out.println(name.block()); // prints null
//        //the second time to put a new value for the same key
//        // emits the previous value -> john
//        Mono<String> name1 = map.put("name", "jane");
//        System.out.println(name.block()); // prints john
//        Mono<String> age = map.put("age", "35");
//        System.out.println(age.block()); // prints null
//        Mono<String> city = map.put("city", "Toronto");
//        System.out.println(city.block()); // prints null

        Map<String, String> javaMap = Map.of(
                "name", "john",
                "age", "35",
                "city", "Toronto"
        );

        //putAll returns a Mono emitting the previous values for the keys
        // (or null if none), not the values just put.
        Mono<Void> putAll = map.putAll(javaMap).then();
        StepVerifier.create(
                //thenMany(Mono.zip(...)) waits for putAll to complete,
                // then retrieves the values for "name", "age", and "city"
                // in parallel and combines them into a tuple.
                putAll.thenMany(
                        Mono.zip(
                                map.get("name"),
                                map.get("age"),
                                map.get("city")
                        )
                )
        )
        //.expectNextMatches(...) asserts that the tuple contains "john", "35",
        // and "Toronto" in the correct order.
        .expectNextMatches(tuple ->
                "john".equals(tuple.getT1()) &&
                "35".equals(tuple.getT2()) &&
                "Toronto".equals(tuple.getT3())
        )
        .verifyComplete();

    }

    @Test
    public void mapJSONObjectTest(){
        // Map<Integer, Student>
        TypedJsonJacksonCodec codec = new TypedJsonJacksonCodec(Integer.class, Student.class);
        RMapReactive<Integer, Student> map = this.client.getMap("users", codec);

        Map<Integer, Student> javaMap = Map.of(
                1, Student.builder().name("jane").age(37).city("Calgary").build(),
                2, Student.builder().name("mike").age(88).city("Woodbridge").build()
        );

        StepVerifier.create(
                        map.putAll(javaMap)
                            .thenMany(
                                Mono.zip(map.get(1),map.get(2))
                            )
                )
                .expectNextMatches(tuple ->
                        "jane".equals(tuple.getT1().getName()) &&
                        37 == tuple.getT1().getAge() &&
                        "Calgary".equals(tuple.getT1().getCity()) &&
                        "mike".equals(tuple.getT2().getName()) &&
                        88 == tuple.getT2().getAge() &&
                        "Woodbridge".equals(tuple.getT2().getCity())
                )
                .verifyComplete();

    }


}
