package com.kakuiwong.gaoyangspringai.ai.tools;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class ComputerAgentTool {

    private final ChatClient computerChatClient;

    public ComputerAgentTool(@Lazy ChatClient computerChatClient) {
        this.computerChatClient = computerChatClient;
    }

    @Tool(description = "你是计算机老师,回答关于计算机相关问题")
    public String getComputerAgent(@ToolParam(description = "用户输入问题") String msg) {
        System.out.println("ComputerAgentTool->" + msg);
        return computerChatClient.prompt(msg).
                call().content();
    }
}

