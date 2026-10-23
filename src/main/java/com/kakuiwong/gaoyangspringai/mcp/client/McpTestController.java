package com.kakuiwong.gaoyangspringai.mcp.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: gaoyang
 * @Description: MCP Client 测试控制器，通过 /mcptest 访问外部 MCP 服务暴露的工具
 */
@RestController
public class McpTestController {

    private final ChatClient mcpChatClient;
    private final ToolCallbackProvider mcpTools;

    public McpTestController(ChatClient.Builder builder,
                             @Qualifier("mcpToolCallbacks") ToolCallbackProvider mcpTools) {
        this.mcpTools = mcpTools;
        this.mcpChatClient = builder
                .defaultSystem("你是一个智能助手，请使用可用的工具来回答用户问题")
                .defaultToolCallbacks(mcpTools)
                .build();
    }

    /**
     * 测试 MCP 工具调用：将用户问题发送给大模型，模型自动选择 MCP 工具执行
     */
    @GetMapping("/mcptest")
    public String mcpTest(@RequestParam String msg) {
        return mcpChatClient.prompt()
                .user(msg)
                .call()
                .content();
    }

    /**
     * 列出外部 MCP Server 提供的所有可用工具名称
     */
    @GetMapping("/mcptest/tools")
    public List<String> listTools() {
        return Arrays.stream(mcpTools.getToolCallbacks())
                .map(cb -> cb.getToolDefinition().name())
                .collect(Collectors.toList());
    }
}
