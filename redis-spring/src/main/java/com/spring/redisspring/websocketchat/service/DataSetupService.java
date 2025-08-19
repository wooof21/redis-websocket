package com.spring.redisspring.websocketchat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Service to set up initial data in the database when app starts,
 * but before the app is ready to serve requests.
 * It reads a SQL schema file and inserts 1000 products into the database.
 */
@Service("chatSqlDataSetupService")
@Slf4j
public class DataSetupService implements CommandLineRunner {


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


        // Low level reactive database API to run the schema creation query
        // Repository does not support schema creation
        this.entityTemplate.getDatabaseClient()
                    .sql(query)
                    .then()
                    .subscribe();

    }
}
