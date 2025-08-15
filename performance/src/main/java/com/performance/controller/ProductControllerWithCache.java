package com.performance.controller;

import com.example.aop.Timing;
import com.performance.model.Product;
import com.performance.service.ProductServiceWithCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("product/v2")
public class ProductControllerWithCache {

    @Autowired
    private ProductServiceWithCache service;

//    @Timing
    @GetMapping("/{id}")
    public Mono<Product> getProduct(@PathVariable int id){
        return this.service.getProduct(id);
    }

//    @Timing
    @PutMapping("/update")
    public Mono<Product> updateProduct(@RequestBody Product product){
        return this.service.updateProduct(product);
    }

//    @Timing
    @DeleteMapping("/{id}")
    public Mono<Boolean> deleteProduct(@PathVariable int id){
        return this.service.deleteProduct(id);
    }

}
