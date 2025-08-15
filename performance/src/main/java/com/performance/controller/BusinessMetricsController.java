package com.performance.controller;

import com.performance.service.BusinessMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("product/metrics")
public class BusinessMetricsController {

    @Autowired
    private BusinessMetricsService metricsService;

    // Return top3Products as a stream
    // Update values every 10 seconds
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<Integer, Double>> getMetrics(){
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(10))
                .flatMap(tick -> metricsService.top3Products())
                .take(10); // limit number of stream return
    }

}
