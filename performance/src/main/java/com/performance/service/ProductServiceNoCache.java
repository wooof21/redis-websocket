package com.performance.service;

import com.performance.model.Product;
import com.performance.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ProductServiceNoCache {

    @Autowired
    private ProductRepository repository;

    public Mono<Product> getProduct(int id){
        return this.repository.findById(id);
    }

    public Mono<Product> updateProduct(Product product){
        return this.repository.findById(product.getId())
                        .switchIfEmpty(Mono.error(new RuntimeException("Product not found")))
                        .flatMap(p -> this.repository.save(product))
                        .thenReturn(product)
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update product", e)));
    }

}
