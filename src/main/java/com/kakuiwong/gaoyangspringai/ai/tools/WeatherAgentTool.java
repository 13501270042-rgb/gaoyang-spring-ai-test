package com.kakuiwong.gaoyangspringai.ai.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class WeatherAgentTool {

    private final ChatClient weatherChatClient;

    public WeatherAgentTool(@Lazy ChatClient weatherChatClient) {
        this.weatherChatClient = weatherChatClient;
    }

    @Tool(description = "天气生活助手,回答关于天气、商场生活等相关问题")
    public String getWeatherAgent(@ToolParam(description = "用户输入问题") String msg) {
        System.out.println("WeatherAgentTool->" + msg);
        return weatherChatClient.prompt(msg).call().content();
    }
}