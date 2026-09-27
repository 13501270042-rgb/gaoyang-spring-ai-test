package com.kakuiwong.gaoyangspringai.ai.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.kakuiwong.gaoyangspringai.ai.tools.WebSearchTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author: gaoyang
 * @Description: AgentScope常量定义
 */
@Configuration
public class HandoffAgentConfig {

    @Bean
    public ReactAgent foodAgent(ChatModel chatModel, WebSearchTool webSearchTool) {
        return ReactAgent.builder()
                .name("foodAgent")
                .model(chatModel)
                .description("仅负责美食推荐、餐厅查询、菜系介绍、小吃探店等问题，不处理任何交通、景点、住宿类问题")
                .instruction("""
                        你是专业美食专家。
                        当用户问具体餐厅、菜品推荐、地区美食等问题时，必须先调用互联网搜索工具获取最新信息，再基于搜索结果回答。
                        不要凭记忆回答，优先使用工具。""")
                .methodTools(webSearchTool)
                .hooks(ModelCallLimitHook.builder().runLimit(10).build())
                .build();
    }

    @Bean
    public ReactAgent travelAgent(ChatModel chatModel, WebSearchTool webSearchTool) {
        return ReactAgent.builder()
                .name("travelAgent")
                .model(chatModel)
                .description("负责景点游览、行程规划、攻略、交通方式、住宿等旅行相关问题，不处理纯美食推荐类问题")
                .instruction("""
                        你是专业旅游专家。
                        当用户询问景点、行程、交通方式、攻略、住宿等旅行相关问题时，必须先调用互联网搜索工具查询最新信息，再基于搜索结果回答。
                        不要凭记忆回答，优先使用工具。""")
                .methodTools(webSearchTool)
                .hooks(ModelCallLimitHook.builder().runLimit(10).build())
                .build();
    }

    @Bean
    public LlmRoutingAgent routerAgent(OllamaChatModel chatModel,
                                       ReactAgent foodAgent,
                                       ReactAgent travelAgent) {
        return LlmRoutingAgent.builder()
                .name("router")
                .description("路由分发，根据用户问题选择子Agent")
                .model(chatModel)
                .systemPrompt("""
                        你是一个路由Agent，根据用户问题选择最合适的一个子Agent。
                        ## 核心规则
                        - 只能选择一个Agent，禁止并行分发到多个Agent
                        - 只返回Agent名称，不要包含其他解释
                        ## 判断逻辑
                        - 用户问“怎么去”、“交通”、“行程”、“景点”、“住宿”、“攻略” → travelAgent
                        - 用户问“吃什么”、“餐厅”、“美食”、“小吃”、“探店” → foodAgent
                        - 涉及旅行目的地的交通、行程等，即使提到地名，也应选 travelAgent
                        ## 示例
                        - “西藏怎么去” → travelAgent
                        - “成都有什么好吃的” → foodAgent
                        - “丽江古城怎么玩” → travelAgent
                        """)
                .subAgents(List.of(foodAgent, travelAgent))
                .hooks(ModelCallLimitHook.builder().runLimit(3).build())
                .build();
    }

}
