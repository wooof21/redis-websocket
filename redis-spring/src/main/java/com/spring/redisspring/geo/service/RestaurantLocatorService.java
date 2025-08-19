package com.spring.redisspring.geo.service;

import com.spring.redisspring.geo.dto.GeoLocation;
import com.spring.redisspring.geo.dto.Restaurant;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeoReactive;
import org.redisson.api.RMapReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.geo.GeoSearchArgs;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Service
public class RestaurantLocatorService {

    private RGeoReactive<Restaurant> geo;
    private RMapReactive<String, GeoLocation> map;

    public RestaurantLocatorService(RedissonReactiveClient client) {
        this.geo = client.getGeo("restaurants", new TypedJsonJacksonCodec(Restaurant.class));
        this.map = client.getMap("usa", new TypedJsonJacksonCodec(String.class, GeoLocation.class));
    }

    public Flux<Restaurant> getRestaurants(String zipcode, int radius, GeoUnit unit) {
        return this.map.get(zipcode)
                .map(gl -> GeoSearchArgs.from(gl.getLongitude(), gl.getLatitude()).radius(radius, unit))
                .flatMap(gs -> this.geo.search(gs))
                .flatMapIterable(Function.identity());
    }

}
