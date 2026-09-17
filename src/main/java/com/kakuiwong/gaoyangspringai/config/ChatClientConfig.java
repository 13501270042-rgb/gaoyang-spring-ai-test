package com.kakuiwong.gaoyangspringai.config;

/**
 * @author: gaoyang
 * @Description:
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    //使用内存
    // @Bean
    // public ChatMemory chatMemory() {
    //     return MessageWindowChatMemory.builder()
    //             .maxMessages(10)
    //             .build();
    // }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient weatherChatClient(OllamaChatModel chatModel,ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你是天气播报员,回答关于天气相关问题")
                .build();
    }

    @Bean
    public ChatClient mathChatClient(OllamaChatModel chatModel,ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你是数学老师,回答关于数学相关问题")
                .build();
    }
}
