package com.performance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Data
@Table("products")
@NoArgsConstructor
@AllArgsConstructor
public class Product  {

    @Id
    private Integer id;
    private String description;
    private double price;

}
