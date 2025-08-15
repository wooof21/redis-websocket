package com.performance.service;

import com.performance.model.Product;
import com.performance.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service to set up initial data in the database when app starts, but before the app is ready to serve requests..
 * It reads a SQL schema file and inserts 1000 products into the database.
 */
@Service
@Slf4j
public class DataSetupService implements CommandLineRunner {

    @Autowired
    private ProductRepository repository;

    // Low-level reactive database API for custom queries, schema creation, etc.
    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @Value("classpath:schema.sql")
    private Resource resource;

    @Override
    public void run(String... args) throws Exception {
        String query = StreamUtils.copyToString(resource.getInputStream(),
                            StandardCharsets.UTF_8);
        log.info("Running query: {}", query);

        Mono<Void> insert = Flux.range(1, 1000)
                .map(i ->
                        new Product(null, "product " + i,
                                // Generate a random price between 1 and 100 Async
                                ThreadLocalRandom.current()
                                        .nextInt(1, 100)))
                .collectList()
                // saveAll returns a Flux<Product> of saved products -> use flatMapMany
                .flatMapMany(l -> this.repository.saveAll(l))
                .then();

        // Low level reactive database API to run the schema creation query
        // Repository does not support schema creation
        this.entityTemplate.getDatabaseClient()
                    .sql(query)
                    .then()// Wait for schema to be ready
                    .then(insert)// Then run insert operation
                    .doFinally(s -> log.info("Data setup completed. {} products inserted.", 1000))
                    .subscribe();

    }
}
