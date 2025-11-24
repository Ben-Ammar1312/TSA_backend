package org.example.tas_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;


@SpringBootApplication
public class TasBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TasBackendApplication.class, args);
    }


    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Buffer request bodies so multiparts include Content-Length (Django rejects chunked uploads)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setBufferRequestBody(true);

        return builder
                .requestFactory(() -> factory)
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120)) // OCR + LLM can take time
                .build();
    }


}
