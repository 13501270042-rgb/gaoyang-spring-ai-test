package com.kakuiwong.gaoyangspringai;

import com.kakuiwong.gaoyangspringai.service.VectorStoreService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * @author: gaoyang
 * @Description: VectorStoreService 测试类
 */
@SpringBootTest
class VectorStoreServiceTest {

    /*
    -- 需要的扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 向量存储表（Spring AI 自动创建）
CREATE TABLE IF NOT EXISTS vector_store (
    id        uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(768)          -- 与 nomic-embed-text 模型维度一致
);

-- HNSW 索引（余弦距离）
CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);*/

    @Resource
    VectorStoreService vectorStoreService;

    @Test
    void testAddText() {
        vectorStoreService.addText("Spring AI是一个用于构建AI应用的框架");
        System.out.println("单条文本写入成功");
    }

    @Test
    void testAddTexts() {
        List<String> texts = List.of(
                "PostgreSQL是一个功能强大的开源关系型数据库",
                "PGVector是PostgreSQL的向量扩展插件,支持相似度检索",
                "Spring AI支持多种向量数据库,包括PGVector、Milvus、Chroma等",
                "向量检索可以用于构建RAG知识库问答系统",
                "Ollama是一个本地运行大模型的工具"
        );
        vectorStoreService.addTexts(texts);
        System.out.println("批量文本写入成功");
    }

    @Test
    void testAddTextWithMetadata() {
        Map<String, Object> metadata = Map.of(
                "source", "test",
                "author", "gaoyang",
                "category", "技术文档"
        );
        vectorStoreService.addTextWithMetadata("带元数据的测试文本,Spring AI向量存储功能演示", metadata);
        System.out.println("带元数据的文本写入成功");
    }

    @Test
    void testSimilaritySearch() {
        // 先写入测试数据
        vectorStoreService.addTexts(List.of(
                "Java是一门面向对象的编程语言",
                "Python适合数据分析和人工智能开发",
                "PostgreSQL支持JSON和向量存储",
                "Spring Boot简化了Java应用的开发流程",
                "Docker容器化技术便于应用部署"
        ));

        // 执行语义检索
        List<Document> results = vectorStoreService.similaritySearch("Java开发框架", 3);
        System.out.println("检索结果:");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ". " + results.get(i).getText());
        }
    }
}
