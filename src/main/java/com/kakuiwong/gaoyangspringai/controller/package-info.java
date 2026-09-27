/**
 * Controller 层知识点总结（gaoyang-spring-ai）
 *
 * <h2>1. SSE 流式接口写法</h2>
 * <ul>
 *   <li>{@code @RestController} + {@code @RequestMapping(produces={"text/event-stream;charset=UTF-8"})} 返回 {@code SseEmitter}</li>
 *   <li>Controller 保持薄层，仅做参数接收和服务委托</li>
 *   <li>{@code @RequestParam(defaultValue="session001")} 提供默认会话ID</li>
 * </ul>
 *
 * <h2>2. SseEmitter 生命周期管理</h2>
 * <ul>
 *   <li>创建时设置超时：{@code new SseEmitter(timeoutMs)}，超时 = 排队等待 + 大模型超时 + 预留缓冲</li>
 *   <li>发送数据：{@code emitter.send(SseEmitter.event().data(content))}</li>
 *   <li>正常结束：{@code emitter.complete()}</li>
 *   <li>异常结束：{@code emitter.completeWithError(e)}</li>
 *   <li>所有 send 操作必须 try-catch IOException</li>
 * </ul>
 *
 * <h2>3. SseEmitter 进阶防护（AtomicBoolean 防竞态）</h2>
 * <ul>
 *   <li>使用 {@code AtomicBoolean emitterCompleted} 标志位跟踪 emitter 是否已关闭</li>
 *   <li>注册 onCompletion/onTimeout/onError 回调统一设置 emitterCompleted=true</li>
 *   <li>onNext 处理开头加 {@code if(emitterCompleted.get()) return;} 守卫拦截</li>
 *   <li>关闭回调使用 {@code compareAndSet(false,true)} 保证只执行一次</li>
 * </ul>
 *
 * <h2>4. 并发排队控制（Redisson）</h2>
 * <ul>
 *   <li>{@code RPermitExpirableSemaphore}：可过期信号量控制并发处理数，租约到期自动回收防止崩溃残留</li>
 *   <li>{@code RScoredSortedSet}（ZSet）：排队队列，score 为入队时间戳，用于超时清理</li>
 *   <li>Lua 脚本原子操作：清理过期占位 + 入队 + 获取队列长度，一次调用完成</li>
 *   <li>排队满直接拒绝，排队超时报错</li>
 * </ul>
 *
 * <h2>5. RAG 回退网络搜索策略</h2>
 * <ul>
 *   <li>优先向量检索知识库（similaritySearch top-K）</li>
 *   <li>知识库无结果时，先用 LLM 提取搜索关键词（异步+超时），再调网络搜索</li>
 *   <li>有参考信息时不注册 Tool（避免模型重复调工具），无参考信息时才注册 Tool</li>
 * </ul>
 *
 * <h2>6. 多层意图识别</h2>
 * <ul>
 *   <li>第一层：关键词字符串包含匹配（最快）</li>
 *   <li>第二层：向量余弦相似度匹配（EmbeddingModel 批量向量化 + 计算余弦相似度，阈值0.8）</li>
 *   <li>第三层：LLM 分类判断（异步线程池 + Future.get 超时兜底）</li>
 * </ul>
 *
 * <h2>7. 手动上下文管理</h2>
 * <ul>
 *   <li>自定义 chat_memory 表存储对话记录（conversationId, content, type, timestamp, sequenceId）</li>
 *   <li>查询最近 N 条历史，反转为时间正序拼接为文本</li>
 *   <li>手动组装 systemPrompt + 参考信息 + 历史对话 + 用户问题</li>
 * </ul>
 *
 * <h2>8. Reactor 流式调用大模型</h2>
 * <pre>{@code
 * chatClient.prompt()
 *     .system(systemPrompt)
 *     .user(userPrompt)
 *     .tools(hasContext ? new Object[]{} : new Object[]{tool})
 *     .stream().content()
 *     .timeout(Duration)
 *     .doOnNext(content -> emitter.send(data))
 *     .doOnComplete(() -> { saveMemory; emitter.complete(); })
 *     .doOnError(e -> emitter.completeWithError(e))
 *     .doFinally(signal -> releaseSemaphore)
 *     .subscribe();
 * }</pre>
 *
 * <h2>9. 关键超时设计常量</h2>
 * <ul>
 *   <li>SSE_TIMEOUT = 排队等待 + 大模型超时 + 预留（约20秒）</li>
 *   <li>SLOT_LEASE > 大模型超时（防止处理中名额被回收）</li>
 *   <li>QUEUE_STALE > 排队等待（残留清理阈值）</li>
 * </ul>
 *
 * @author gaoyang
 */
package com.kakuiwong.gaoyangspringai.controller;
