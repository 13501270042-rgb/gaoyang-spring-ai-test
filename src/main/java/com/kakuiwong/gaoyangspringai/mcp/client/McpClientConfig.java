package com.kakuiwong.gaoyangspringai.mcp.client;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: gaoyang
 * @Description: MCP Client 配置类，通过 McpSyncHttpClientRequestCustomizer
 * 为 Streamable HTTP 请求注入认证 Token 头信息
 */
@Configuration
public class McpClientConfig {

    @Value("${mcp.auth.token}")
    private String mcpAuthToken;

    /**
     * 注册请求定制器，Spring AI 自动配置会自动检测并应用到 Streamable HTTP 传输层，
     * 每次向外部 MCP Server 发送请求时自动携带 Authorization: Bearer token
     */
    @Bean
    public McpSyncHttpClientRequestCustomizer mcpHttpClientRequestCustomizer() {
        return (builder, method, endpoint, body, context) ->
                builder.header("Authorization", "Bearer " + mcpAuthToken);
    }
}
