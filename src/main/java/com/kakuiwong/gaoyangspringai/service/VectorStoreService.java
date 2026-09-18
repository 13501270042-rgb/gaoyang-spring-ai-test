package com.kakuiwong.gaoyangspringai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author: gaoyang
 * @Description: 向量化Service,将字符串写入PG向量数据库并支持检索
 */
@Service
public class VectorStoreService {

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 将文本向量化后写入PG向量数据库
     *
     * @param text 要向量化的文本内容
     */
    public void addText(String text) {
        Document document = new Document(text);
        vectorStore.add(List.of(document));
    }

    /**
     * 批量将文本向量化后写入PG向量数据库
     *
     * @param texts 文本内容列表
     */
    public void addTexts(List<String> texts) {
        List<Document> documents = texts.stream()
                .map(Document::new)
                .toList();
        vectorStore.add(documents);
    }

    /**
     * 将文本向量化后写入PG向量数据库(带自定义元数据)
     *
     * @param text     文本内容
     * @param metadata 元数据
     */
    public void addTextWithMetadata(String text, Map<String, Object> metadata) {
        Document document = new Document(text, metadata);
        vectorStore.add(List.of(document));
    }

    /**
     * 根据查询文本进行语义相似度检索
     *
     * @param query 查询文本
     * @param topK  返回最相似的结果数量
     * @return 相似的文档列表
     */
    public List<Document> similaritySearch(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );
    }
}
