package com.spring.redisspring.geo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Builder
public class GeoLocation {

    private double longitude;
    private double latitude;

}
