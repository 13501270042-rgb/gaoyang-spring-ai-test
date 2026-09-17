package com.kakuiwong.gaoyangspringai;

import org.springframework.ai.model.chat.memory.repository.jdbc.autoconfigure.JdbcChatMemoryRepositoryAutoConfiguration;
import org.springframework.ai.model.chat.memory.repository.redis.autoconfigure.RedisChatMemoryRepositoryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        JdbcChatMemoryRepositoryAutoConfiguration.class,
        RedisChatMemoryRepositoryAutoConfiguration.class})
public class GaoyangSpringAiApplication {


    public static void main(String[] args) {
        SpringApplication.run(GaoyangSpringAiApplication.class, args);
    }

}
