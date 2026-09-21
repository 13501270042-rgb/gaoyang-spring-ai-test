package com.kakuiwong.gaoyangspringai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class ChatTestController {

    @Autowired
    ChatClient supervisorChatClient;

    /*
                用户输入提示词
                        ↓
                【系统提示词 + 用户提示词 + 历史对话】送入大模型
                        ↓
                模型判断：是否需要外部信息？
                        ├─ 不需要 → 直接生成答案返回用户
                        └─ 需要 → 第一步调用RAG检索私有知识库
                                ↓
                RAG返回文档片段
                                ↓
                判断RAG信息是否足够？
                                ├─ 足够 → 把RAG文档拼入上下文，生成答案
                                └─ 不足 → 调用【网络查询工具】获取互联网最新信息
                                        ↓
                搜索结果拼入上下文
                                        ↓
                大模型整合全部信息，输出最终回答给用户
*/

    @RequestMapping(value = "/hello", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter hello(String msg,
                            @RequestParam(defaultValue = "session001") String sessionId) {
        SseEmitter emitter = new SseEmitter(60 * 3 * 1000L);

        supervisorChatClient.prompt(msg).advisors(
                        a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(60 * 3))
                .subscribe(
                        content -> {
                            try {
                                emitter.send(SseEmitter.event().data(content));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> emitter.completeWithError(error),
                        emitter::complete
                );

        return emitter;
    }
}
