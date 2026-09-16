package com.kakuiwong.gaoyangspringai.config;

/**
 * @author: gaoyang
 * @Description:
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    /**
     * 内存会话记忆，最多保留10条消息，重启丢失
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory()).build())
                .defaultSystem("你是小高机器人,回答简洁专业")
                .build();
    }
}
