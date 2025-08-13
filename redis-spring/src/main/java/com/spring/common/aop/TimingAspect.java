package com.spring.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class TimingAspect {

    @Around("@annotation(com.spring.common.aop.Timing)")
    public Object timeSpent(ProceedingJoinPoint point) throws Throwable{
        // ProceedingJoinPoint allows to proceed with the method execution
        // and also gives access to the method signature and arguments.
        // It is a special type of JoinPoint that can be used to intercept method calls.
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        // MethodSignature provides access to the method being intercepted.
        // It allows to get the method name, return type, parameter types, etc.
        Object result = point.proceed();

        // If result type is Mono
        if (result instanceof reactor.core.publisher.Mono) {
            return ((reactor.core.publisher.Mono<?>) result)
                    // The function is executed when the Mono is subscribed to.
                    .doOnSubscribe(s -> log.info("TimingAspect - Method: {} - started", method.getName()))
                    // .elapsed() returns a Mono that emits a tuple containing the elapsed time and the original value.
                    .elapsed()
                    // Logs the elapsed time for each signal.
                    // getT1() returns the elapsed time in milliseconds (Long) since the previous signal (or since subscription for the first item).
                    .doOnNext(tuple -> log.info("TimingAspect - Method: {} - took: {} ms", method.getName(), tuple.getT1()))
                    // Extracts the original value from the tuple.
                    // getT2() returns the original value emitted by the source Mono.
                    .map(tuple -> tuple.getT2());
        // If result type is Flux
        } else if (result instanceof reactor.core.publisher.Flux) {
            return ((reactor.core.publisher.Flux<?>) result)
                    .doOnSubscribe(s -> log.info("TimingAspect - Method: {} - started", method.getName()))
                    .elapsed()
                    .doOnNext(tuple -> log.info("TimingAspect - Method: {} - took: {} ms", method.getName(), tuple.getT1()))
                    .map(tuple -> tuple.getT2());
        // If method call is not reactive
        } else {
            long start = System.currentTimeMillis();
            long end = System.currentTimeMillis();
            log.info("TimingAspect - Method: {} - took: {} ms", method.getName(), end - start);
            return result;
        }
    }
}
