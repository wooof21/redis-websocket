package com.performance.service;

import com.performance.model.Product;
import com.performance.util.CacheTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ProductServiceWithCache {

    @Autowired
    private CacheTemplate<Integer, Product> cacheTemplate;

    @Autowired
    private ProductVisitService visitService;

    // GET
    public Mono<Product> getProduct(int id){
        return this.cacheTemplate.get(id)
                                .doFirst(() -> this.visitService.addVisit(id))
                ;
    }

    // PUT
    public Mono<Product> updateProduct(Product product){
        return this.cacheTemplate.update(product.getId(), product);
    }

    // DELETE
    public Mono<Boolean> deleteProduct(int id){
        return this.cacheTemplate.delete(id);
    }

    // INSERT


}
