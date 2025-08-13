package com.spring.redisspring.city.service;

import com.spring.redisspring.city.client.CityClient;
import com.spring.redisspring.city.model.City;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCacheReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.spring.common.CacheNameConstants.CITY_WEATHER_CACHE_KEY;

@Service
@Slf4j
public class CityService {

    @Autowired
    private CityClient cityClient;

    private final RMapReactive<String, City> cityMap;
    // Use this RMapCacheReactive to enable for individual cache entries to expire
//    private final RMapCacheReactive<String, City> cityMapCache;

    public CityService(RedissonReactiveClient client) {
        this.cityMap = client.getMap(CITY_WEATHER_CACHE_KEY,
                new TypedJsonJacksonCodec(String.class, City.class));

//        this.cityMapCache = client.getMapCache(CITY_WEATHER_CACHE_KEY,
//                new TypedJsonJacksonCodec(String.class, City.class));;
    }

    /**
     * For Reactive service call, manually check the cache first
     * and if not found, then call the external service.
     */
    public Mono<City> getCity(final String zipCode){
//        return this.cityMapCache.get(zipCode)
//                .switchIfEmpty(this.cityClient.getCity(zipCode)
//                        .flatMap(city -> this.cityMapCache
//                                .fastPut(zipCode, city, 10, TimeUnit.SECONDS)
//                                .thenReturn(city)));

        return this.cityMap.get(zipCode)
                .switchIfEmpty(this.cityClient.getCity(zipCode)
                                    .flatMap(city -> this.cityMap.fastPut(zipCode, city).thenReturn(city)))
                // When error occurs, fallback to the external service
                .onErrorResume(ex -> this.cityClient.getCity(zipCode));
    }

    /**
     * Use a CRON job to get all city weather data from the external service
     * and update the cache.
     * This is useful for the initial load of the cache.
     */
//    @Scheduled(fixedRate = 10_000)
    public void updateCity(){
        this.cityClient.getAll()
                .collectList()
                .map(list ->
                        list.stream().collect(Collectors.toMap(City::getZip, Function.identity())))
                .flatMap(this.cityMap::putAll)
//                .flatMap(this.cityMapCache::putAll)
                // Remember to subscribe to Flux/Mono to trigger the execution
                .doOnSuccess(aVoid -> log.info("City cache updated done."))
                .doOnError(ex -> log.error("Error updating city cache: ", ex))
                .subscribe();
    }

}
