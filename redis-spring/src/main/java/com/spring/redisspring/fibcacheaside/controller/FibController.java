package com.spring.redisspring.fibcacheaside.controller;

import com.example.aop.Timing;
import com.spring.redisspring.fibcacheaside.service.FibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("fib")
public class FibController {

    @Autowired
    private FibService service;

    @Timing
    @GetMapping("/{index}/{name}")
    public Mono<Integer> getFib(@PathVariable int index, @PathVariable String name){
        /**
         *  Mono.fromSupplier is a way to wrap a synchronous computation in a lazy, reactive Mono.
         *   - Takes a Supplier<T> — a function that returns a value when called.
         *   - Lazy: The supplier is not executed until someone subscribes to the Mono.
         *   - If the supplier throws an exception, the Mono will emit an error.
         *
         *    - To defer execution of a blocking or CPU-bound operation until subscription.
         *    - To wrap non-reactive code so it fits into a reactive pipeline.
         *    - To prevent work from running if nobody consumes the result.
         */

        // Lazily generating data when API is called
        return Mono.fromSupplier(() -> this.service.getFib(index, name));
    }

    @GetMapping("/{index}/delete")
    public Mono<Void> evictByKey(@PathVariable int index){
        return Mono.fromRunnable(() -> this.service.evictByKey(index));
    }

}
