package com.kakuiwong.gaoyangspringai.ai.tools;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author: gaoyang
 * @Description:
 */
@Component
public class ComputerTool {

    @Tool(description = "计算机知识库,可以查询计算机相关概念和知识,包括硬件组成、内存、鼠标、键盘等计算机领域知识")
    public String getComputer(@ToolParam(description = "用户的问题或关键词，例如：计算机组成、内存、鼠标") String name) {
        System.out.println("ComputerTool->" + name);
        if (name.contains("计算机组成")) {
            return "MathAddTool->鼠标,键盘等";
        } else if (name.contains("鼠标")) {
            return "MathAddTool->鼠标,不错";
        }
        return "MathAddTool->"  + "没有这个书";
    }
}

