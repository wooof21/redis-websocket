package com.spring.redisspring.weather.service;

import com.spring.common.aop.Timing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.stream.IntStream;

@Service
public class WeatherService {

    @Autowired
    private ExternalServiceClient client;

    /**
     * Since the weather is updated every 20 seconds,
     * and CachePut each time the weather is updated,
     *
     * even the method is returning 0,
     * every time when call the API and invoke this method,
     * the weather info will be retrieved from the cache,
     * 0 is never returned.
     */
    @Cacheable(cacheNames = "weather", cacheResolver = "redisCacheNameResolver")
    public int getWeatherFromExternalOrCache(int zip){
        return 0;
    }

    // Worked as a pre-computing task to update the weather info
    // and store it in the cache.
    // @Scheduled(fixedRate = 20_000)
    public void updateWeather(){
        System.out.println("Updating weather.");
        IntStream.rangeClosed(1, 5)
                .forEach(this.client::getWeatherInfo);
    }

}
