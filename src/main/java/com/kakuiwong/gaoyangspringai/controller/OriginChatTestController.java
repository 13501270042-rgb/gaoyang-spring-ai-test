package com.kakuiwong.gaoyangspringai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakuiwong.gaoyangspringai.ai.tools.LocalWeatherTool;
import com.kakuiwong.gaoyangspringai.entity.SpringAiChatMemory;
import com.kakuiwong.gaoyangspringai.mapper.SpringAiChatMemoryMapper;
import com.kakuiwong.gaoyangspringai.service.VectorStoreService;
import com.kakuiwong.gaoyangspringai.service.WebSearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: gaoyang
 * @Description: 手动整合：系统提示词 + 上下文管理(最大10条) + 向量检索(RAG) + SSE流式输出
 */
@RestController
public class OriginChatTestController {

    @Autowired
    ChatClient originChatClient;
    @Autowired
    private VectorStoreService vectorStoreService;
    @Autowired
    SpringAiChatMemoryMapper springAiChatMemoryMapper;
    @Autowired
    LocalWeatherTool localWeatherTool;
    @Autowired
    WebSearchService webSearchService;

    @RequestMapping(value = "/origin/hello", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter originHello(String msg,
                                  @RequestParam(defaultValue = "session001") String sessionId) {
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        // ===== 1. 手动添加系统提示词 =====
        String systemPrompt = "你是一个智能助手，请根据提供的上下文信息和对话历史，准确回答用户问题。";

        // ===== 2. 向量查询(RAG) - 检索相关知识，为空时回退到网络搜索 =====
        //可写为Tool,让大模型自动判断是否调用网络搜索,开源SearXNG
        String ragContext = searchKnowledge(msg);
        String sourceLabel = "知识库参考信息";
        if (ragContext.isEmpty()) {
            ragContext = webSearchService.search(msg);
            sourceLabel = "网络搜索参考信息";
            System.out.println("RAG无结果，启用网络搜索: " + ragContext);
        }

        // ===== 3. 上下文管理 - 获取最近10条对话记录 =====
        List<SpringAiChatMemory> history = getRecentMessages(sessionId, 10);
        String historyText = buildHistoryText(history);

        // ===== 4. 手动组装完整提示词 =====
        StringBuilder userPrompt = new StringBuilder();
        if (!ragContext.isEmpty()) {
            userPrompt.append("【" + sourceLabel + "】\n").append(ragContext).append("\n\n");
        }
        if (!historyText.isEmpty()) {
            userPrompt.append("【历史对话记录】\n").append(historyText).append("\n\n");
        }
        userPrompt.append("【用户当前问题】\n").append(msg);

        System.out.println("userPrompt->" + userPrompt);

        // ===== 5. 保存用户消息到记忆表 =====
        saveMessage(sessionId, msg, MessageType.USER.getValue().toUpperCase());

        // ===== 6. 调用大模型并流式返回 =====
        final StringBuilder fullResponse = new StringBuilder();

        originChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt.toString())
                //添加工具
                .tools(localWeatherTool)
                .stream()
                .content()
                .timeout(Duration.ofMinutes(3))
                .doOnNext(content -> {
                    fullResponse.append(content);
                    try {
                        emitter.send(SseEmitter.event().data(content));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(
                        // 7. 流结束后保存AI回复到记忆表
                        () -> {
                            saveMessage(sessionId, fullResponse.toString(), MessageType.ASSISTANT.getValue().toUpperCase());
                            emitter.complete();
                        })
                .doOnError(e -> emitter.completeWithError(e))
                .subscribe();

        return emitter;
    }

    /**
     * 获取最近N条对话记录
     */
    private List<SpringAiChatMemory> getRecentMessages(String sessionId, int maxMessages) {
        return springAiChatMemoryMapper.selectList(
                new LambdaQueryWrapper<SpringAiChatMemory>()
                        .eq(SpringAiChatMemory::getConversationId, sessionId)
                        .orderByDesc(SpringAiChatMemory::getTimestamp)
                        .orderByDesc(SpringAiChatMemory::getSequenceId)
                        .last("LIMIT " + maxMessages)
        );
    }

    /**
     * 构建对话历史文本(按时间正序展示)
     */
    private String buildHistoryText(List<SpringAiChatMemory> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        // 反转为时间正序
        Collections.reverse(messages);
        return messages.stream()
                .map(m -> {
                    String role = "USER".equals(m.getType()) ? "用户" : "助手";
                    return role + ": " + m.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 向量检索知识库，返回相关文档文本
     */
    private String searchKnowledge(String query) {
        List<Document> docs = vectorStoreService.similaritySearch(query, 3);
        if (docs == null || docs.isEmpty()) {
            return "";
        }
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 保存消息到chat_memory表(自动计算sequence_id)
     */
    private void saveMessage(String sessionId, String content, String type) {
        // 查询当前会话消息总数作为sequence_id
        Long maxSeq = springAiChatMemoryMapper.selectCount(
                new LambdaQueryWrapper<SpringAiChatMemory>()
                        .eq(SpringAiChatMemory::getConversationId, sessionId)
        );
        long nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

        SpringAiChatMemory memory = new SpringAiChatMemory();
        memory.setConversationId(sessionId);
        memory.setContent(content);
        memory.setType(type);
        memory.setTimestamp(LocalDateTime.now());
        memory.setSequenceId(nextSeq);
        springAiChatMemoryMapper.insert(memory);
    }
}
