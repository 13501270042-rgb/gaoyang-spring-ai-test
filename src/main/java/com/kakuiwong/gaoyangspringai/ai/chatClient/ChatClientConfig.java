package com.kakuiwong.gaoyangspringai.ai.chatClient;

/**
 * @author: gaoyang
 * @Description:
 */

import com.kakuiwong.gaoyangspringai.ai.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;
    @Value("${spring.ai.ollama.chat.model}")
    private String model;
    @Value("${spring.ai.ollama.chat.temperature}")
    private Double temperature;
    @Value("${spring.ai.ollama.chat.num-predict}")
    private Integer numPredict;

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
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return ex -> {
            // 这里可以打点、脱敏、映射错误码
            log.warn("[tool-exec] {}", ex.getMessage());
            return "ERROR: 工具执行失败(code=TOOL_EXEC_FAILURE)，请稍后再试。";
        };
    }

    @Bean
    public OllamaChatModel ollamaChatModel(ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMinutes(5));

        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        OllamaChatOptions defaultOptions = OllamaChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .numPredict(numPredict)
                .build();

        // 将自定义 ToolExecutionExceptionProcessor 接入 ToolCallingManager
        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                .toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(defaultOptions)
                .toolCallingManager(toolCallingManager)
                .build();
    }

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
                .defaultSystem("你是任务调度主管,根据用户需求,调用合适的子Agent完成任务。" +
                        "当用户问题涉及知识库内容时,使用知识库检索工具查询。汇总所有结果返回给用户")
                .defaultTools(computerAgentTool, weatherAgentTool, knowledgeSearchTool)
                .build();
    }

    @Bean
    public ChatClient originChatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }
}
