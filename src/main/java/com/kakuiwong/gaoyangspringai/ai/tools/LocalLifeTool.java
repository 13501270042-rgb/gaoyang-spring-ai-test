package com.kakuiwong.gaoyangspringai.ai.tools;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class LocalLifeTool {

    @Tool(description = "查询商场特色，输入商场名称，返回内容")
    public String getLife(@ToolParam(description = "商场名称，例如：万达") String mall) {
        System.out.println("LocalLifeTool->" + mall);
        if (mall.contains("万达")) {
            return "LocalLifeTool->有万达影城";
        } else if (mall.contains("荟聚")) {
            return "LocalLifeTool->停车不要钱";
        }
        return "LocalLifeTool->各种好吃的";
    }
}

