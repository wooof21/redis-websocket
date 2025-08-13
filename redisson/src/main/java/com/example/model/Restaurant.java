package com.example.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class Restaurant {

    private String id;
    private String city;
    private double latitude;
    private double longitude;
    private String name;
    private String zip;

}
