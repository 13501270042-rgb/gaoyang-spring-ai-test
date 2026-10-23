package com.kakuiwong.gaoyangspringai.mcp;

import com.kakuiwong.gaoyangspringai.ai.tools.LocalLifeTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: gaoyang
 * @Description: MCP Server 配置类，将 @Tool 注解的方法注册为 MCP 工具
 */
@Configuration
public class McpServerConfig {

    @Value("${mcp.auth.token}")
    private String mcpAuthToken;

    /**
     * 将 LocalLifeTool 注册为 MCP 工具提供者
     * MCP Server starter 会自动发现 ToolCallbackProvider 中的 @Tool 方法并暴露为 MCP 工具
     */
    @Bean
    public ToolCallbackProvider localLifeToolCallbackProvider(LocalLifeTool localLifeTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(localLifeTool)
                .build();
    }

    /**
     * 注册 MCP 凭证校验过滤器，仅拦截 SSE 端点和消息端点
     * 客户端必须在请求头中携带 Authorization: Bearer <token>
     */
    @Bean
    public FilterRegistrationBean<McpAuthFilter> mcpAuthFilterRegistration() {
        FilterRegistrationBean<McpAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new McpAuthFilter(mcpAuthToken));
        // 仅拦截 MCP 相关端点：SSE 连接端点 + 消息端点
        registration.addUrlPatterns("/sse", "/mcp/messages");
        registration.setOrder(1);
        registration.setName("mcpAuthFilter");
        return registration;
    }
}
