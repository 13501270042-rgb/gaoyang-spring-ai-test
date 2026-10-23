package com.kakuiwong.gaoyangspringai.ai.tools;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class McpLocalLifeTool {

    @Tool(description = "查询商场特色，输入商场名称，返回内容")
    public String getLife(@ToolParam(description = "商场名称，例如：万达") String mall) {
        System.out.println("McpLocalLifeTool->" + mall);
        if (mall.contains("万达")) {
            return "McpLocalLifeTool->有万达影城";
        } else if (mall.contains("荟聚")) {
            throw new RuntimeException("荟聚出现错误");
        }
        return "McpLocalLifeTool->各种好吃的";
    }
}

