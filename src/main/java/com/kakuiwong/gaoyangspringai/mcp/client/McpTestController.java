package com.kakuiwong.gaoyangspringai.mcp.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: gaoyang
 * @Description: MCP Client 测试控制器，演示三种多 MCP 服务使用方式：
 * 1. /mcptest         — 聚合所有 MCP 服务的工具（默认）
 * 2. /mcptest/life    — 仅使用 life 服务的工具
 * 3. /mcptest/dynamic — 通过参数动态指定使用哪个 MCP 服务
 */
@RestController
public class McpTestController {

    @Autowired
    ChatClient lifeChatClient;


    /**
     * 仅使用 life 服务的工具回答问题
     */
    @GetMapping("/mcptest/life")
    public String lifeChat(@RequestParam String msg) {
        return lifeChatClient.prompt()
                .user(msg)
                .call()
                .content();
    }

    /**
     * 仅使用 weather 服务的工具回答问题
     */
    // @GetMapping("/mcptest/weather")
    // public String weatherChat(@RequestParam String msg) {
    //     return weatherClientByMcp.prompt()
    //             .user(msg)
    //             .call()
    //             .content();
    // }
}
