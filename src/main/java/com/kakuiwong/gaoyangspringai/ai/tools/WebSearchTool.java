package com.kakuiwong.gaoyangspringai.ai.tools;

import com.kakuiwong.gaoyangspringai.service.WebSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description: 互联网实时搜索工具
 */
@Component
public class WebSearchTool {

    @Autowired
    WebSearchService webSearchService;

    @Tool(description = "用于互联网实时搜索，可查询旅游攻略、交通方式、美食推荐、景点信息、最新新闻、事实数据等任何需要实时信息的问题")
    public String webSearch(@ToolParam(description = "搜索关键词或搜索语句，例如：去西藏的交通方式、成都必吃美食")
                            String query) {
        System.out.println("========= WebSearchTool: " + query);
        return webSearchService.search(query);
    }
}
