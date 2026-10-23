package com.kakuiwong.gaoyangspringai.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: gaoyang
 * @Description: MCP Client 配置类，手动创建以连接名为 Bean 名的 McpSyncClient，
 *               支持通过 @Qualifier("连接名") 精确使用特定连接
 *
 * <p>使用方式：</p>
 * <ul>
 *   <li>@Qualifier("gaoyang-local") → 仅使用 gaoyang-local 连接的工具</li>
 *   <li>@Qualifier("another-server") → 仅使用 another-server 连接的工具</li>
 *   <li>注入 McpSyncClient（无 Qualifier）→ 使用所有连接的工具（聚合模式）</li>
 * </ul>
 */
@Configuration
public class McpClientConfig {

    /** 保存所有手动创建的客户端，用于应用关闭时统一释放 */
    private final List<McpSyncClient> allClients = new ArrayList<>();

    // ==================== 按连接名创建独立 McpSyncClient Bean ====================

    /**
     * 连接 1：gaoyang-local（本地 MCP Server，生活服务工具）
     * 使用 @Qualifier("gaoyang-local") 注入
     */
    @Bean("gaoyang-local")
    public McpSyncClient gaoyangLocalMcpClient() {
        var transport = HttpClientSseClientTransport.builder("http://localhost:8080")
                .customizeRequest(request -> request.header("Authorization", "Bearer 123"))
                .build();
        var client = McpClient.sync(transport).build();
        allClients.add(client);
        return client;
    }

    /**
     * 连接 2：another-server（外部 MCP Server，按需启用）
     * 使用 @Qualifier("another-server") 注入
     */
    // @Bean("another-server")
    // public McpSyncClient anotherServerMcpClient() {
    //     var transport = HttpClientSseClientTransport.builder("http://localhost:9090")
    //             .customizeRequest(request -> request.header("Authorization", "Bearer 456"))
    //             .build();
    //     var client = McpClient.sync(transport).build();
    //     allClients.add(client);
    //     return client;
    // }

    // ==================== 聚合 ChatClient（使用所有连接） ====================

    /**
     * 聚合所有连接的 ChatClient（默认 Bean，无需 @Qualifier）
     */
    @Bean
    public ChatClient mcpChatClient(ChatClient.Builder chatClientBuilder,
                                    @Qualifier("gaoyang-local") McpSyncClient localClient) {
        ToolCallbackProvider mcpRemoteTools = new SyncMcpToolCallbackProvider(localClient);
        return chatClientBuilder
                .defaultToolCallbacks(mcpRemoteTools)
                .build();
    }

    // ==================== 分离 ChatClient（按连接名精确使用） ====================

    /**
     * 仅使用 gaoyang-local 连接的工具
     * 在 Controller 中通过 @Qualifier("localChatClient") 注入
     */
    // @Bean("localChatClient")
    // public ChatClient localChatClient(ChatClient.Builder chatClientBuilder,
    //                                   @Qualifier("gaoyang-local") McpSyncClient localClient) {
    //     ToolCallbackProvider tools = new SyncMcpToolCallbackProvider(localClient);
    //     return chatClientBuilder
    //             .defaultToolCallbacks(tools)
    //             .build();
    // }

    /**
     * 仅使用 another-server 连接的工具
     * 在 Controller 中通过 @Qualifier("anotherChatClient") 注入
     */
    // @Bean("anotherChatClient")
    // public ChatClient anotherChatClient(ChatClient.Builder chatClientBuilder,
    //                                     @Qualifier("another-server") McpSyncClient anotherClient) {
    //     ToolCallbackProvider tools = new SyncMcpToolCallbackProvider(anotherClient);
    //     return chatClientBuilder
    //             .defaultToolCallbacks(tools)
    //             .build();
    // }

    // ==================== 生命周期管理 ====================

    @PreDestroy
    public void cleanup() {
        allClients.forEach(client -> {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        });
    }
}
