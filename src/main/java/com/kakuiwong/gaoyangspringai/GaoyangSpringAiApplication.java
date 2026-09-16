package com.kakuiwong.gaoyangspringai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.model.chat.memory.repository.jdbc.autoconfigure.JdbcChatMemoryRepositoryAutoConfiguration;
import org.springframework.ai.model.chat.memory.repository.redis.autoconfigure.RedisChatMemoryRepositoryAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        JdbcChatMemoryRepositoryAutoConfiguration.class,
        RedisChatMemoryRepositoryAutoConfiguration.class})
public class GaoyangSpringAiApplication {


    public static void main(String[] args) {
        SpringApplication.run(GaoyangSpringAiApplication.class, args);
    }

}
