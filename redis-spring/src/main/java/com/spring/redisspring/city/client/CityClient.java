package com.spring.redisspring.city.client;

import com.spring.redisspring.city.model.City;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CityClient {

    private final WebClient webClient;

    public CityClient(@Value("${city.service.url}") String url) {
        this.webClient = WebClient.builder()
                                .baseUrl(url)
                                .build();
    }

    public Mono<City> getCity(final String zipCode){
        return this.webClient
                        .get()
                        .uri("{zipcode}", zipCode)
                        .retrieve()
                        .bodyToMono(City.class);
    }

    // get all city weather data without passing a zip code
    public Flux<City> getAll(){
        return this.webClient
                .get()
                .retrieve()
                .bodyToFlux(City.class);
    }

}
