package com.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.redisson.api.RedissonReactiveClient;
import reactor.test.StepVerifier;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    private final RedissonConfig config = new RedissonConfig();
    protected RedissonReactiveClient client;

    @BeforeAll
    public void setClient(){
        this.client = this.config.getRedissonReactiveClient();
    }

    @AfterAll
    public void shutdown(){
        this.config.getClient().shutdown();
    }

    protected void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
