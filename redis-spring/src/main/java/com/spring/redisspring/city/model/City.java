package com.spring.redisspring.city.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class City {

    private String zip;
    private String city;
    private String stateName;
    private int temperature;

}

/*

{
   "zip":"10001",
   "lat":40.75065,
   "lng":-73.99718,
   "city":"New York",
   "stateId":"NY",
   "stateName":"New York",
   "population":24117,
   "density":15153.7,
   "temperature":74
}

 */