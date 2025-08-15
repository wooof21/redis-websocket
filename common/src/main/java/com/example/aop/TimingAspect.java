package com.example.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class TimingAspect {

    @Around("@annotation(com.example.aop.Timing)")
    public Object timeSpent(ProceedingJoinPoint point) throws Throwable{
        // ProceedingJoinPoint allows to proceed with the method execution
        // and also gives access to the method signature and arguments.
        // It is a special type of JoinPoint that can be used to intercept method calls.
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        // MethodSignature provides access to the method being intercepted.
        // It allows to get the method name, return type, parameter types, etc.
        Object result = point.proceed();
        long startTime = System.currentTimeMillis();

        /**
         * elapsed() works on each emitted signal. For Mono, it will emit a single tuple when the Mono completes.
         * For Flux, it emits per element.
         *
         * When under high load with JMeter:
         *  - doOnSubscribe and elapsed() do not block, but logging itself (log.info) is synchronous.
         *  - If Netty threads are saturated and logging is slow, the reactive pipeline can be backed up,
         *    which eventually causes request timeouts.
         *
         * To avoid blocking the reactive pipeline with logging:
         *  - doFinally ensures measure total execution time when the reactive stream completes, errors, or is cancelled.
         *  - Logging runs on boundedElastic scheduler to avoid blocking Netty threads.
         *  - Avoids elapsed() per element, which could generate unnecessary tuples for a Flux with many elements
         *
         */
        // If result type is Mono
        if (result instanceof Mono<?> mono) {
            return mono
                    .doFinally(signalType ->
                            Schedulers.boundedElastic().schedule(() -> {
                                long duration = System.currentTimeMillis() - startTime;
                                log.info("TimingAspect - Method: {} - finished, took {} ms", method.getName(), duration);
                            })
                    );
        // If result type is Flux
        } else if (result instanceof Flux<?> flux) {
            return flux
                    .doFinally(signalType ->
                            Schedulers.boundedElastic().schedule(() -> {
                                long duration = System.currentTimeMillis() - startTime;
                                log.info("TimingAspect - Method: {} - finished, took {} ms", method.getName(), duration);
                            })
                    );
        // If method call is not reactive
        } else {
            long start = System.currentTimeMillis();
            long end = System.currentTimeMillis();
            log.info("TimingAspect - Method: {} - took: {} ms", method.getName(), end - start);
            return result;
        }
    }
}
