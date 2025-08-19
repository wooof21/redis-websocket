package com.spring.redisspring.geo.service;

import com.spring.redisspring.geo.dto.GeoLocation;
import com.spring.redisspring.geo.dto.Restaurant;
import com.spring.redisspring.geo.util.RestaurantUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RGeoReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service("restaurantDataSetupService")
@Slf4j
public class DataSetupService implements CommandLineRunner {

    private RGeoReactive<Restaurant> geo;
    private RMapReactive<String, GeoLocation> map;

    @Autowired
    private RedissonReactiveClient client;

    @Override
    public void run(String... args) throws Exception {
        this.geo = this.client.getGeo("restaurants", new TypedJsonJacksonCodec(Restaurant.class));
        this.map = this.client.getMap("usa", new TypedJsonJacksonCodec(String.class, GeoLocation.class));

        Flux.fromIterable(RestaurantUtil.getRestaurants())
                .flatMap(r -> this.geo.add(r.getLongitude(), r.getLatitude(), r).thenReturn(r))
                .flatMap(r -> this.map.fastPut(r.getZip(), GeoLocation.of(r.getLongitude(), r.getLatitude())))
                .doFinally(s -> log.info("Restaurant Data Setup - {}", s))
                .subscribe();
    }

}
