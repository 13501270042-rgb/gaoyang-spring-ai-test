package com.kakuiwong.gaoyangspringai;

import org.springframework.ai.model.chat.memory.repository.redis.autoconfigure.RedisChatMemoryRepositoryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        RedisChatMemoryRepositoryAutoConfiguration.class})
public class GaoyangSpringAiApplication {


    public static void main(String[] args) {
        SpringApplication.run(GaoyangSpringAiApplication.class, args);
    }

}
