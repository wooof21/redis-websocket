package com.spring.redisspring.geo.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.redisspring.geo.dto.Restaurant;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class RestaurantUtil {

    public static List<Restaurant> getRestaurants(){
        ObjectMapper mapper = new ObjectMapper();
        InputStream stream = RestaurantUtil.class.getClassLoader().getResourceAsStream("./static/restaurant.json");
        try {
            return mapper.readValue(stream, new TypeReference<List<Restaurant>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

}
