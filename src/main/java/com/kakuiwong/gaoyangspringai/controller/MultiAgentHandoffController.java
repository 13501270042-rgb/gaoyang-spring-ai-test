package com.kakuiwong.gaoyangspringai.controller;

import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: gaoyang
 * @Description: 多智能体Handoff交互控制器
 */
@Slf4j
@RestController
public class MultiAgentHandoffController {

    @Autowired
    LlmRoutingAgent llmRoutingAgent;

    @GetMapping(value = "/chat", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter chat(@RequestParam String msg) throws GraphRunnerException {

        SseEmitter sseEmitter = new SseEmitter(Duration.ofSeconds(30).toMillis());

        // 原子标志位：跟踪emitter是否已关闭，防止对已完成的emitter调用send/complete
        AtomicBoolean emitterCompleted = new AtomicBoolean(false);

        sseEmitter.onCompletion(() -> {
            emitterCompleted.set(true);
            log.info("SSE连接完成, msg={}", msg);
        });
        sseEmitter.onTimeout(() -> {
            emitterCompleted.set(true);
            log.warn("SSE连接超时, msg={}", msg);
        });
        sseEmitter.onError(e -> {
            emitterCompleted.set(true);
            log.error("SSE连接异常, msg={}", msg, e);
        });


        llmRoutingAgent.stream(msg)
                .timeout(Duration.ofMinutes(1))
                .doOnError(e -> log.error(">>> Flux 错误信号", e))
                .doOnComplete(() -> log.info(">>> Flux 完成信号"))
                .doOnCancel(() -> log.warn(">>> Flux 被取消"))
                .subscribe(
                        output -> {
                            if (emitterCompleted.get()) {
                                return;
                            }
                            try {
                                if (output instanceof StreamingOutput streamingOutput) {
                                    OutputType type = streamingOutput.getOutputType();

                                    if (type == OutputType.AGENT_MODEL_STREAMING) {
                                        Message message = streamingOutput.message();
                                        if (message instanceof AssistantMessage assistantMessage) {
                                            String text = assistantMessage.getText();
                                            if (text != null && !text.isEmpty()) {
                                                sseEmitter.send(SseEmitter.event().data(text));
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("SSE发送数据失败, msg={}", msg, ex);
                                if (emitterCompleted.compareAndSet(false, true)) {
                                    try {
                                        sseEmitter.completeWithError(ex);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        },
                        error -> {
                            log.error("Agent流式执行异常", error);
                            if (emitterCompleted.compareAndSet(false, true)) {
                                try {
                                    sseEmitter.send(SseEmitter.event().data("执行异常: " + error.getMessage()));
                                } catch (IOException ignored) {
                                }
                                try {
                                    sseEmitter.completeWithError(error);
                                } catch (Exception ignored) {
                                }
                            }
                        },
                        () -> {
                            if (emitterCompleted.compareAndSet(false, true)) {
                                try {
                                    sseEmitter.complete();
                                } catch (Exception ignored) {
                                }
                                log.info("Agent流式执行完成");
                            }
                        }
                );
        return sseEmitter;
    }
}
