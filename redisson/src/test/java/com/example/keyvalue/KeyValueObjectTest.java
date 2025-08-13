package com.example.keyvalue;

import com.example.BaseTest;
import com.example.model.Student;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucketReactive;
import org.redisson.codec.TypedJsonJacksonCodec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


public class KeyValueObjectTest extends BaseTest {

    @Test
    public void keyValueObjectTest(){
        Student student = Student.builder()
                .name("john")
                .age(35)
                .city("Toronto")
                .build();
        RBucketReactive<Student> bucket =
                this.client.getBucket("student:1",
                        new TypedJsonJacksonCodec(Student.class));
        Mono<Void> set = bucket.set(student);
        Mono<Student> get = bucket.get()
                .doOnNext(System.out::println);
        StepVerifier.create(set.then(get))
                .expectNextMatches(s ->
                        s.getName().equals("john") &&
                        s.getAge() == 35 &&
                        s.getCity().equals("Toronto"))
                .verifyComplete();
    }

}
