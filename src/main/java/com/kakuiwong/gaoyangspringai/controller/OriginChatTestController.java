package com.kakuiwong.gaoyangspringai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakuiwong.gaoyangspringai.ai.tools.LocalWeatherTool;
import com.kakuiwong.gaoyangspringai.entity.SpringAiChatMemory;
import com.kakuiwong.gaoyangspringai.mapper.SpringAiChatMemoryMapper;
import com.kakuiwong.gaoyangspringai.service.VectorStoreService;
import com.kakuiwong.gaoyangspringai.service.WebSearchService;
import com.kakuiwong.gaoyangspringai.util.ThreadPoolUtil;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
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
import java.util.UUID;
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

    //处理名额key(并发处理1个,名额带租约自动过期,防止进程崩溃导致名额永久占用)
    private static final String CHAT_PERMIT_KEY = "origin:chat:permit";
    // 排队占位集合key(成员带入队时间戳,崩溃残留按时间自动清理)
    private static final String CHAT_QUEUE_KEY = "origin:chat:queue:members";
    // 最大排队数
    private static final int MAX_QUEUE_SIZE = 2;
    //大模型超时
    private static final Duration LLM_TIMEOUT_SECONDS = Duration.ofMinutes(3);
    //排队最长等待时间(秒)
    private static final long QUEUE_WAIT_SECONDS = 3 * 60;
    //流式返回超时,大模型超时+排队超时+预留
    private static final long SSE_TIMEOUT_MILLISECOND = QUEUE_WAIT_SECONDS + 3 * 60 * 1000L + 10000L;
    //处理名额租约,大于大模型返回
    private static final long SLOT_LEASE_SECONDS = 3 * 60 + 10L;
    // 排队残留阈值
    private static final long QUEUE_STALE_SECONDS = QUEUE_WAIT_SECONDS + 10L;

    @RequestMapping(value = "/origin/hello", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter originHello(String msg,
                                  @RequestParam(defaultValue = "session001") String sessionId) throws IOException {

        System.out.println("================================进入方法");


        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLISECOND);

        // ===== 0. Redisson排队控制:并发处理1个,最多排队MAX_QUEUE_SIZE个 =====
        // 可过期信号量:名额带租约,进程意外退出未释放时,租约到期自动回收,名额不会永久占用
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(CHAT_PERMIT_KEY);
        semaphore.trySetPermits(1);

        // 立即尝试获取处理名额(成功返回permitId,失败返回null)
        String permitId = null;
        try {
            permitId = semaphore.tryAcquire(0, SLOT_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (permitId != null) {
            // 有空闲处理名额,直接处理
            final String acquiredPermitId = permitId;
            ThreadPoolUtil.getPool().submit(() -> {
                processChat(emitter, msg, sessionId, semaphore, acquiredPermitId);
            });
            return emitter;
        }

        // 无空闲名额,判断是否可排队,排队成功后若未主动出队,超时QUEUE_STALE_SECONDS自动出队
        String requestId = UUID.randomUUID().toString();
        Long waiting = this.atomicEnqueueAndGetQueueSize(CHAT_QUEUE_KEY, requestId, QUEUE_STALE_SECONDS);

        // 队伍满,直接返回
        if (waiting > MAX_QUEUE_SIZE) {
            ThreadPoolUtil.getPool().execute(() -> {
                removeQueueMember(CHAT_QUEUE_KEY, requestId);
                try {
                    emitter.send(SseEmitter.event().data("队列已满，请稍后再试"));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    return;
                }
                emitter.complete();
            });
            return emitter;
        }

        // 需要排队,直接返回排队提示及等待人数,后台异步等待处理名额
        ThreadPoolUtil.getPool().submit(() -> {
            try {
                emitter.send(SseEmitter.event().data("排队中，请等待，当前等待人数: " + waiting));
            } catch (IOException e) {
                emitter.completeWithError(e);
                return;
            }
            String queuedPermitId = null;
            try {
                // 排队等待处理名额
                //QUEUE_WAIT_SECONDS,等待许可时间
                //SLOT_LEASE_SECONDS,许可自动过期时间
                queuedPermitId = semaphore.tryAcquire(QUEUE_WAIT_SECONDS, SLOT_LEASE_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 无论排队成功还是超时,都离开队伍;程序崩溃走不到这里也没关系,残留会被时间戳清理
                removeQueueMember(CHAT_QUEUE_KEY, requestId);
            }
            if (queuedPermitId != null) {
                // 轮到当前请求,开始处理
                processChat(emitter, msg, sessionId, semaphore, queuedPermitId);
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
    private void processChat(SseEmitter emitter, String msg, String sessionId,
                             RPermitExpirableSemaphore semaphore, String permitId) {
        // 名额只释放一次(完成/异常/取消只触发其中一个);即使全部没执行,租约到期名额也会自动回收
        AtomicBoolean released = new AtomicBoolean(false);
        Runnable releaseSlot = () -> {
            if (released.compareAndSet(false, true)) {
                semaphore.tryRelease(permitId);
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
                    .timeout(LLM_TIMEOUT_SECONDS)
                    .doOnNext(content -> {
                        fullResponse.append(content);
                        try {
                            emitter.send(SseEmitter.event().data(content));
                        } catch (Exception e) {
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

    /**
     * 原子入队：清理过期占位，新增请求，返回入队后的排队数量
     *
     * @param queueKey     zset队列key
     * @param requestId    请求唯一id
     * @param staleSeconds 占位超时秒数
     * @return 入队后当前排队数量
     */
    public Long atomicEnqueueAndGetQueueSize(String queueKey, String requestId, long staleSeconds) {
        long now = System.currentTimeMillis();
        long staleThreshold = now - staleSeconds * 1000;

        String lua = """
                local k = KEYS[1]
                local stale = tonumber(ARGV[1])
                local score = tonumber(ARGV[2])
                redis.call('ZREMRANGEBYSCORE', k, '-inf', stale)
                redis.call('ZADD', k, score, ARGV[3])
                return redis.call('ZCARD', k)
                """;

        return redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                lua,
                RScript.ReturnType.LONG,
                Collections.singletonList(queueKey),
                String.valueOf(staleThreshold),
                String.valueOf(now),
                requestId
        );
    }


    public boolean removeQueueMember(String queueKey, String requestId) {
        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey, StringCodec.INSTANCE);
        return queue.remove(requestId);
    }
}
