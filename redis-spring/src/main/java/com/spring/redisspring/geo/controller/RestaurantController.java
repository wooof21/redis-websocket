package com.spring.redisspring.geo.controller;

import com.spring.redisspring.geo.dto.Restaurant;
import com.spring.redisspring.geo.service.RestaurantLocatorService;
import org.redisson.api.GeoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("geo")
public class RestaurantController {

    @Autowired
    private RestaurantLocatorService locatorService;

    @GetMapping("/{zip}")
    public Flux<Restaurant> getRestaurants(@PathVariable String zip,
                                           @RequestParam(name = "radius") int radius,
                                           @RequestParam(name = "unit") String unit){
        return this.locatorService.getRestaurants(zip, radius, GeoUnit.valueOf(unit));
    }

}
