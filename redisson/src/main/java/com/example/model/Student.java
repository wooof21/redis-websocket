package com.example.model;

import lombok.*;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    private String name;
    private int age;
    private String city;

}
