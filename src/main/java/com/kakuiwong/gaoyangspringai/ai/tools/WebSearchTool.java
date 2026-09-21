package com.kakuiwong.gaoyangspringai.ai.tools;

import com.kakuiwong.gaoyangspringai.service.WebSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class WebSearchTool {

    @Autowired
    WebSearchService webSearchService;

    @Tool(description = "用于互联网实时搜索，当需要最新信息、事实数据时调用，传入搜索query")
    public String getWeatherAgent(@ToolParam(description = "搜索关键词或搜索语句，由模型根据用户问题生成")
                                  String query) {
        return webSearchService.search(query);
    }
}