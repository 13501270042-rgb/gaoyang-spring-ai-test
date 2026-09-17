package com.kakuiwong.gaoyangspringai.tools;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class LocalWeatherTool {

    @Tool(description = "查询指定城市当前天气，输入城市中文名称，返回天气和温度")
    public String getWeather(@ToolParam(description = "城市名称，例如：北京、上海") String city) {
        if ("北京".equals(city)) {
            return "北京，晴，26℃";
        } else if ("廊坊".equals(city)) {
            return "廊坊，阴，18℃";
        }
        return city + "：多云，24℃";
    }
}

