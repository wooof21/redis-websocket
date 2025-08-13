package com.example.map;

import com.example.BaseTest;
import com.example.RedissonConfig;
import com.example.model.Student;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * LocalCachedMap is a special type of map that maintains a local cache of
 * frequently accessed data - avoid network calls to Redis server
 *
 * This can significantly improve read performance by reducing the need
 * to access the Redis server for every read operation.
 *
 * The local cache is kept in sync with the Redis server using
 * various synchronization strategies - sync the data from Redis to local memory
 *
 * When there is an update in Redis
 * - update the local cache immediately - Redis server publishes an event
 *   and all LocalCachedMap instances receive the event and update their local cache
 * - invalidate the local cache entry so that the next read will fetch the updated data from Redis
 * - or keep the local cache unchanged and let it expire after a specified time
 */
@Execution(ExecutionMode.CONCURRENT)
public class LocalCachedMapTest extends BaseTest {

    private RLocalCachedMap<Integer, Student> studentsMap;


    @BeforeAll
    public void setupClient(){
        RedissonConfig config = new RedissonConfig();
        RedissonClient client = config.getClient();

        LocalCachedMapOptions<Integer, Student> mapOptions =
                // students - key name of the map
                LocalCachedMapOptions.<Integer, Student>name("students")
                    .codec(new TypedJsonJacksonCodec(Integer.class, Student.class))
                    .syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE)
//                    .cacheSize(1000)
//                    .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
                    // CLEAR: on reconnection, clear the local cache and fetch the data from Redis
                    // RELOAD: on reconnection, reload the local cache from Redis
                    // NONE: on reconnection, do not clear or reload the local cache
                    .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);

        this.studentsMap = client.getLocalCachedMap(mapOptions);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void appServer1(){
        Student student1 = new Student("john", 35, "Toronto");
        Student student2 = new Student("jane", 44, "Calgary");

        this.studentsMap.put(1, student1);
        this.studentsMap.put(2, student2);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void appServer2(){
        sleep(3000);
        Student student1 = new Student("john-new", 35, "Toronto");
        this.studentsMap.put(1, student1);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void appServer3(){
        Flux.interval(Duration.ofSeconds(1))
                .doOnNext(i -> System.out.println(i + " ==> " + studentsMap.get(1)))
                .subscribe();

        sleep(10000);
    }


}
