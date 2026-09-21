package com.kakuiwong.gaoyangspringai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: gaoyang
 * @Description: 基于SearXNG的网络搜索服务
 */
@Service
public class WebSearchService {

    @Value("${searxng.base-url:http://localhost:8888}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 使用SearXNG进行网络搜索
     *
     * @param query 搜索关键词
     * @return 搜索结果的文本摘要列表
     */
    // public List<String> search(String query) {
    //     return search(query, 5);
    // }

    public String search(String query) {
        return search(query, 5).stream()
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 使用SearXNG进行网络搜索
     *
     * @param query      搜索关键词
     * @param maxResults 最大返回结果数
     * @return 搜索结果的文本摘要列表
     */
    public List<String> search(String query, int maxResults) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = baseUrl + "/search?q=" + encodedQuery + "&format=json&number=" + maxResults;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("SearXNG搜索失败，状态码: " + response.statusCode());
                return List.of();
            }

            return parseResults(response.body(), maxResults);
        } catch (Exception e) {
            System.err.println("SearXNG搜索异常: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 搜索并将结果拼接为一段文本，方便作为LLM上下文
     *
     * @param query      搜索关键词
     * @param maxResults 最大结果数
     * @return 拼接后的搜索结果文本
     */
    public String searchAsContext(String query, int maxResults) {
        List<String> results = search(query, maxResults);
        if (results.isEmpty()) {
            return "未找到相关搜索结果。";
        }
        StringBuilder sb = new StringBuilder("搜索结果：\n");
        for (int i = 0; i < results.size(); i++) {
            sb.append(i + 1).append(". ").append(results.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析SearXNG返回的JSON结果
     */
    private List<String> parseResults(String json, int maxResults) {
        List<String> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode resultsNode = root.get("results");
            if (resultsNode == null || !resultsNode.isArray()) {
                return results;
            }
            int count = 0;
            for (JsonNode item : resultsNode) {
                if (count >= maxResults) break;
                String title = item.has("title") ? item.get("title").asText() : "";
                String content = item.has("content") ? item.get("content").asText() : "";
                String url = item.has("url") ? item.get("url").asText() : "";
                String result = title + " - " + content + " (来源: " + url + ")";
                results.add(result);
                count++;
            }
        } catch (Exception e) {
            System.err.println("解析SearXNG结果失败: " + e.getMessage());
        }
        return results;
    }
}
