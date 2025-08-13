package com.spring.redisspring;

import org.junit.jupiter.api.RepeatedTest;
import org.redisson.api.RAtomicLongReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Using: implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
 *
 * Spring Redis Template Runs: 13368 ms
 * Spring Redis Template Runs: 11978 ms
 * Spring Redis Template Runs: 11628 ms
 *
 * Using: implementation 'org.redisson:redisson:3.50.0'
 *
 * Redisson Runs: 12678 ms
 * Redisson Runs: 11528 ms
 * Redisson Runs: 11537 ms
 */
@SpringBootTest
class RedisSpringApplicationTests {

	// spring redis template - similar to redisson
	// redisson - more features, support distributed lock, distributed collections
	// spring redis template - only support basic data structure
	@Autowired
	private ReactiveStringRedisTemplate template;

	@Autowired
	private RedissonReactiveClient client;

	@RepeatedTest(3)
	public void springDataRedisTest() {
		ReactiveValueOperations<String, String> valueOperations = this.template.opsForValue();
		long before = System.currentTimeMillis();
		Mono<Void> mono = Flux.range(1, 500_000)
				.flatMap(i -> valueOperations.increment("user:1:visit")) // incr
				.then();
		StepVerifier.create(mono)
				.verifyComplete();
		long after = System.currentTimeMillis();
		System.out.println("Spring Redis Template Runs: " + (after - before) + " ms");
	}

	@RepeatedTest(3)
	public void redissonTest() {
		RAtomicLongReactive atomicLong = this.client.getAtomicLong("user:2:visit");
		long before = System.currentTimeMillis();
		Mono<Void> mono = Flux.range(1, 500_000)
				.flatMap(i -> atomicLong.incrementAndGet()) // incr
				.then();
		StepVerifier.create(mono)
				.verifyComplete();
		long after = System.currentTimeMillis();
		System.out.println("Redisson Runs: " + (after - before) + " ms");
	}

}
