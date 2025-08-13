package com.example.utils;

import com.example.model.Restaurant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class RestaurantUtil {

    public static List<Restaurant> getRestaurants(){
        ObjectMapper mapper = new ObjectMapper();
        InputStream stream = RestaurantUtil.class.getClassLoader().getResourceAsStream("restaurant.json");
        try {
            return mapper.readValue(stream, new TypeReference<List<Restaurant>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

}
