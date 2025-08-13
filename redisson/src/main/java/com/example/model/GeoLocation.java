package com.example.model;

import lombok.*;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Builder
public class GeoLocation {

    private double longitude;
    private double latitude;

}
