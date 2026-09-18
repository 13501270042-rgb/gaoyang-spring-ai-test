package com.kakuiwong.gaoyangspringai.ai.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: gaoyang
 * @Description: 向量化工具,将字符串写入PG向量数据库
 */
@Component
public class VectorStoreTool {

    private final VectorStore vectorStore;

    public VectorStoreTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "将文本内容向量化后存储到PG向量数据库中,用于后续语义检索")
    public String addTextToVector(@ToolParam(description = "要向量化的文本内容") String text) {
        Document document = new Document(text);
        vectorStore.add(List.of(document));
        return "文本已成功写入PG向量数据库";
    }
}
