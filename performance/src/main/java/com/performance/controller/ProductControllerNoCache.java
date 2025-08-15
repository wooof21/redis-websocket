package com.performance.controller;

import com.example.aop.Timing;
import com.performance.model.Product;
import com.performance.service.ProductServiceNoCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("product/v1")
public class ProductControllerNoCache {

    @Autowired
    private ProductServiceNoCache service;

    @Timing
    @GetMapping("/{id}")
    public Mono<Product> getProduct(@PathVariable int id){
        return this.service.getProduct(id);
    }

    @Timing
    @PutMapping("/update")
    public Mono<Product> updateProduct(@RequestBody Product product){
        return this.service.updateProduct(product);
    }

}
