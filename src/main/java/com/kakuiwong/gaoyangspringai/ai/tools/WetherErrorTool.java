package com.kakuiwong.gaoyangspringai.ai.tools;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class WetherErrorTool {

    @Tool(description = "查询天气,参数是城市名称,返回内容")
    public String getError(@ToolParam(description = "城市名称，例如：北京") String city) {
        System.out.println("ErrorTool->" + city);
        if (city.contains("北京")) {
            return "北京37.5度";
        } else if (city.contains("廊坊")) {
            throw new RuntimeException("廊坊发生错误");
        }
        return "没查到";
    }
}

