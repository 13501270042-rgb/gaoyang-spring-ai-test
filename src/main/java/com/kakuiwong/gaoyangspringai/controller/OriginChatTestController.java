package com.kakuiwong.gaoyangspringai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakuiwong.gaoyangspringai.ai.tools.LocalWeatherTool;
import com.kakuiwong.gaoyangspringai.entity.SpringAiChatMemory;
import com.kakuiwong.gaoyangspringai.mapper.SpringAiChatMemoryMapper;
import com.kakuiwong.gaoyangspringai.service.VectorStoreService;
import com.kakuiwong.gaoyangspringai.service.WebSearchService;
import com.kakuiwong.gaoyangspringai.util.ThreadPoolUtil;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author: gaoyang
 * @Description: 手动整合
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
    @Autowired
    RedissonClient redissonClient;

    /** 处理名额信号量key(并发处理1个) */
    private static final String CHAT_SEMAPHORE_KEY = "origin:chat:semaphore";
    /** 排队人数计数key */
    private static final String CHAT_QUEUE_COUNT_KEY = "origin:chat:queue:count";
    /** 最大排队数 */
    private static final int MAX_QUEUE_SIZE = 3;
    /** 排队最长等待时间(秒),需小于SseEmitter超时时间 */
    private static final long QUEUE_WAIT_SECONDS = 120;

    @RequestMapping(value = "/origin/hello", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter originHello(String msg,
                                  @RequestParam(defaultValue = "session001") String sessionId) throws IOException {
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);

        // ===== 0. Redisson排队控制:并发处理1个,最多排队5个 =====
        RSemaphore semaphore = redissonClient.getSemaphore(CHAT_SEMAPHORE_KEY);
        semaphore.trySetPermits(1);

        if (semaphore.tryAcquire()) {
            // 有空闲处理名额,直接处理
            processChat(emitter, msg, sessionId, semaphore);
            return emitter;
        }

        // 无空闲名额,判断是否可排队
        RAtomicLong queueCount = redissonClient.getAtomicLong(CHAT_QUEUE_COUNT_KEY);
        long waiting = queueCount.incrementAndGet();
        if (waiting > MAX_QUEUE_SIZE) {
            // 队伍满,直接返回
            queueCount.decrementAndGet();
            emitter.send(SseEmitter.event().data("队列已满，请稍后再试"));
            emitter.complete();
            return emitter;
        }

        // 需要排队,直接返回排队提示及等待人数,后台异步等待处理名额
        emitter.send(SseEmitter.event().data("排队中，请等待，当前等待人数: " + waiting));
        ThreadPoolUtil.getPool().submit(() -> {
            boolean acquired = false;
            try {
                acquired = semaphore.tryAcquire(QUEUE_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 无论排队成功还是超时,都离开队伍
                queueCount.decrementAndGet();
            }
            if (acquired) {
                // 轮到当前请求,开始处理
                processChat(emitter, msg, sessionId, semaphore);
            } else {
                // 排队超时,直接返回
                try {
                    emitter.send(SseEmitter.event().data("排队超时，请稍后再试"));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    /**
     * 抢到处理名额后执行:组装提示词并调用大模型流式返回,结束后释放处理名额
     */
    private void processChat(SseEmitter emitter, String msg, String sessionId, RSemaphore semaphore) {
        // 名额只释放一次(完成/异常/取消只触发其中一个)
        AtomicBoolean released = new AtomicBoolean(false);
        Runnable releaseSlot = () -> {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        };
        try {
            // ===== 1. 手动添加系统提示词 =====
            String systemPrompt = "你是一个智能助手。\n" +
                    "规则：\n" +
                    "1. 如果上下文中提供了【知识库参考信息】或【网络搜索参考信息】，请优先使用这些参考信息来回答。\n" +
                    "2. 只有当参考信息无法回答用户问题时，才允许调用工具获取更多信息。\n" +
                    "3. 不要重复调用工具获取参考信息中已有的内容。";

            // ===== 2. 向量查询(RAG) - 检索相关知识 =====
            String ragContext = searchKnowledge(msg);
            String sourceLabel = "知识库参考信息";

            // ===== 3. 向量查询不到,网络搜索 =====
            //可写为Tool,让大模型自动判断是否调用网络搜索,开源SearXNG
            if (ragContext.isEmpty()) {
                //优化搜索关键词为多个网络搜索词
                String webQuery = getWebQueryKeyword(msg);
                ragContext = webSearchService.search(webQuery);
                sourceLabel = "网络搜索参考信息";
                System.out.println("RAG无结果，启用网络搜索: " + ragContext);
            }

            // ===== 4. 上下文管理 - 获取最近10条对话记录 =====
            List<SpringAiChatMemory> history = getRecentMessages(sessionId, 10);
            String historyText = buildHistoryText(history);

            // ===== 5. 手动组装完整提示词 =====
            boolean hasContext = !ragContext.isEmpty();
            StringBuilder userPrompt = new StringBuilder();
            if (!ragContext.isEmpty()) {
                userPrompt.append("【" + sourceLabel + "】\n").append(ragContext).append("\n\n");
            }
            if (!historyText.isEmpty()) {
                userPrompt.append("【历史对话记录】\n").append(historyText).append("\n\n");
            }
            userPrompt.append("【用户当前问题】\n").append(msg);

            System.out.println("userPrompt->" + userPrompt);

            // ===== 6. 保存用户消息到记忆表 =====
            saveMessage(sessionId, msg, MessageType.USER.getValue().toUpperCase());

            // ===== 7. 调用大模型并流式返回 =====
            final StringBuffer fullResponse = new StringBuffer();

            originChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt.toString())
                    //有参考信息时不注册工具，避免模型重复调用工具；无参考信息时才注册工具
                    .tools(hasContext ? new Object[]{} : new Object[]{localWeatherTool})
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
                            // 8. 流结束后保存AI回复到记忆表
                            () -> {
                                saveMessage(sessionId, fullResponse.toString(), MessageType.ASSISTANT.getValue().toUpperCase());
                                emitter.complete();
                            })
                    .doOnError(e -> emitter.completeWithError(e))
                    .doFinally(signal -> releaseSlot.run())
                    .subscribe();
        } catch (Exception e) {
            emitter.completeWithError(e);
            releaseSlot.run();
        }
    }

    /**
     * 提取搜索关键词
     */
    private String getWebQueryKeyword(String msg) {
        try {
            String prompt = "你是关键词提取器，把用户问题转换成 1~3 条适合搜索引擎的查询词，逗号分隔，不要多余文字。用户问题：" + msg;
            Future<String> future = ThreadPoolUtil.getPool().submit(
                    () -> originChatClient.prompt(prompt).call().content());

            String result = future.get(10, TimeUnit.SECONDS);
            System.out.println("搜索关键词->" + result);
            return result;
        } catch (Exception e) {
        }
        return msg;
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
