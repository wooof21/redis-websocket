package com.spring;

import com.example.aop.TimingAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties
@Import(TimingAspect.class) // Manually bring in the shared beans
public class RedisSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisSpringApplication.class, args);
	}

}
