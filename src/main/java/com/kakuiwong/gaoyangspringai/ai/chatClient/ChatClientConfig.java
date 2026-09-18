package com.kakuiwong.gaoyangspringai.ai.chatClient;

/**
 * @author: gaoyang
 * @Description:
 */

import com.kakuiwong.gaoyangspringai.ai.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Autowired
    LocalWeatherTool localWeatherTool;
    @Autowired
    LocalLifeTool localLifeTool;
    @Autowired
    ComputerTool computerTool;
    @Autowired
    ComputerAgentTool computerAgentTool;
    @Autowired
    WeatherAgentTool weatherAgentTool;
    @Autowired
    KnowledgeSearchTool knowledgeSearchTool;

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient computerChatClient(OllamaChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                // .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你是计算机老师,回答相关问题")
                .defaultTools(computerTool)
                .build();
    }

    @Bean
    public ChatClient weatherChatClient(OllamaChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                //.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你是机器人客服,尽量使用自带工具查询结果,回答用户相关问题")
                .defaultTools(localWeatherTool, localLifeTool)
                .build();
    }

    @Bean
    public ChatClient supervisorChatClient(OllamaChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你是任务调度主管，根据用户需求，调用合适的子Agent完成任务。" +
                        "当用户问题涉及知识库内容时，使用知识库检索工具查询。汇总所有结果返回给用户")
                .defaultTools(computerAgentTool, weatherAgentTool, knowledgeSearchTool)
                .build();
    }
}
