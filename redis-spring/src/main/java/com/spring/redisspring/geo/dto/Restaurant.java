package com.spring.redisspring.geo.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    private String id;
    private String city;
    private double latitude;
    private double longitude;
    private String name;
    private String zip;

}
