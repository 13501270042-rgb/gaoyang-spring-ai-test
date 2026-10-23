package com.kakuiwong.gaoyangspringai.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author: gaoyang
 * @Description: 多 MCP 服务配置类，为每个 MCP Server 连接创建独立的 ChatClient，
 * 实现工具隔离：每个 ChatClient 只能使用对应 MCP 服务的工具
 *
 * 注意：Spring AI MCP Client 自动配置只创建 List<McpSyncClient>，
 * 不会为每个命名连接创建带 @Qualifier 的独立 bean，
 * 因此需要手动创建 McpSyncClient bean 以实现按连接隔离。
 */
@Configuration
public class MultiMcpConfig {

    @Value("${mcp.auth.token}")
    private String mcpAuthToken;

    /**
     * 手动创建 life 连接的 McpSyncClient bean，
     * bean 名称即方法名 lifeMcpClient，注入点通过 @Qualifier("lifeMcpClient") 引用
     */
    @Bean
    public McpSyncClient lifeMcpClient() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:8088")
                .endpoint("/mcp")
                .customizeRequest(request -> request.header("Authorization", "Bearer " + mcpAuthToken))
                .build();
        return McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("gaoyang-mcp-client", "1.0.0"))
                .requestTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 生活服务 ChatClient：仅使用 life 连接（localhost:8088）的工具
     */
    @Bean
    public ChatClient lifeChatClient(ChatClient.Builder builder,
                                     @Qualifier("lifeMcpClient") McpSyncClient lifeMcpClient) {
        ToolCallbackProvider lifeTools = new SyncMcpToolCallbackProvider(lifeMcpClient);
        return builder
                .defaultSystem("你是生活服务助手，请使用生活服务相关的工具来回答用户问题")
                .defaultToolCallbacks(lifeTools)
                .build();
    }



    /**
     * 天气服务 ChatClient：仅使用 weather 连接（localhost:8089）的工具
     */
    // @Bean
    // public ChatClient weatherChatClient(ChatClient.Builder builder,
    //                                     @Qualifier("weather") McpSyncClient weatherMcpClient) {
    //     ToolCallbackProvider weatherTools = new SyncMcpToolCallbackProvider(weatherMcpClient);
    //     return builder
    //             .defaultSystem("你是天气查询助手，请使用天气相关的工具来回答用户问题")
    //             .defaultToolCallbacks(weatherTools)
    //             .build();
    // }
}
