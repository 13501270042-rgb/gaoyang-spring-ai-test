package com.kakuiwong.gaoyangspringai.mcp.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: gaoyang
 * @Description: MCP Client 聊天控制器，通过 ChatClient 调用 MCP Server 暴露的工具
 *
 * <p>聚合模式：注入默认的 mcpChatClient，模型自动选择所有连接的工具</p>
 * <p>分离模式：通过 @Qualifier 注入指定的 ChatClient，精确控制使用哪个 Server 的工具</p>
 */
@RestController
@RequestMapping("/mcp-chat")
public class McpChatController {

    // ========== 聚合模式：使用所有连接的工具 ==========
    @Autowired
    ChatClient mcpChatClient;

    // ========== 分离模式（按需启用）：取消注释以使用特定连接 ==========
    // private final ChatClient localChatClient;
    // private final ChatClient anotherChatClient;

    /**
     * 分离模式构造器（按需启用）
     * 启用后需同时在 McpClientConfig 中取消分离模式的注释
     */
    // public McpChatController(@Qualifier("localChatClient") ChatClient localChatClient,
    //                          @Qualifier("anotherChatClient") ChatClient anotherChatClient) {
    //     this.localChatClient = localChatClient;
    //     this.anotherChatClient = anotherChatClient;
    // }

    /**
     * 聚合模式：所有 MCP 连接的工具都可用，模型自动选择
     */
    @GetMapping
    public String chat(@RequestParam String msg) {
        return mcpChatClient.prompt()
                .user(msg)
                .call()
                .content();
    }

    // ========== 分离模式接口（按需启用） ==========

    /**
     * 仅使用第一个连接（gaoyang-local）的工具
     */
    // @GetMapping("/local")
    // public String localChat(@RequestParam String msg) {
    //     return localChatClient.prompt()
    //             .user(msg)
    //             .call()
    //             .content();
    // }

    /**
     * 仅使用第二个连接（another-server）的工具
     */
    // @GetMapping("/another")
    // public String anotherChat(@RequestParam String msg) {
    //     return anotherChatClient.prompt()
    //             .user(msg)
    //             .call()
    //             .content();
    // }
}
