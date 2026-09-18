package com.kakuiwong.gaoyangspringai.ai.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: gaoyang
 * @Description: 知识库检索工具,从PG向量数据库中检索相关知识
 */
@Component
public class KnowledgeSearchTool {

    private final VectorStore vectorStore;

    public KnowledgeSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "从知识库中检索与用户问题相关的内容,当用户询问知识库中已有的信息时调用此工具")
    public String searchKnowledge(@ToolParam(description = "用户的查询问题") String query) {
        System.out.println("KnowledgeSearchTool->" + query);
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(3)
                        .build()
        );
        if (results.isEmpty()) {
            return "知识库中未找到相关信息";
        }
        String content = results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
        System.out.println("KnowledgeSearchTool结果->" + content);
        return content;
    }
}
