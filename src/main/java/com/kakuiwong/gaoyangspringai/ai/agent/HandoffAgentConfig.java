package com.kakuiwong.gaoyangspringai.ai.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author: gaoyang
 * @Description: AgentScope常量定义
 */
@Configuration
public class HandoffAgentConfig {

    // 美食Agent
    @Bean
    public ReactAgent foodAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("foodAgent")
                .model(chatModel)
                .description("负责美食、餐厅、菜系、小吃、探店问题")
                .instruction("你是专业美食专家，简洁回答代码相关问题。")
                .build();
    }

    @Bean
    public ReactAgent travelAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("travelAgent")
                .model(chatModel)
                .description("负责景点、行程、攻略、交通、住宿。")
                .instruction("你是专业旅游专家，简洁回答代码相关问题。")
                .build();
    }

    @Bean
    public LlmRoutingAgent routerAgent(ChatModel chatModel,
                                       ReactAgent foodAgent,
                                       ReactAgent travelAgent) {
        return LlmRoutingAgent.builder()
                .name("router")
                .description("路由分发，根据用户问题选择子Agent")
                .model(chatModel)
                .systemPrompt("""
                        你是一个路由Agent，根据用户问题选择最合适的子Agent。
                        ## 可用子Agent
                        ### foodAgent
                        - 功能：美食、餐厅、菜系、小吃、探店
                        ### travelAgent  
                        - 功能：景点、行程、攻略、交通、住宿
                        ## 响应格式
                        只返回Agent名称，不要包含其他解释。
                        """)
                .subAgents(List.of(foodAgent, travelAgent))
                .build();
    }

}
