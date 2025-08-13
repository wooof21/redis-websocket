package com.spring.redisspring.weather.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class ExternalServiceClient {

    @CachePut(cacheNames = "weather", cacheResolver = "redisCacheNameResolver", key = "#zip")
    public int getWeatherInfo(int zip){
        // external service call to get weather info
        int newWeather = ThreadLocalRandom.current().nextInt(60, 100);
        log.info("Updating weather for zip {} to {}", zip, newWeather);
        return newWeather;
    }

}
