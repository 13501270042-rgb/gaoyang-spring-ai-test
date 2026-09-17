package com.kakuiwong.gaoyangspringai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class ChatController {

    @Autowired
    ChatClient weatherChatClient;

    @RequestMapping(value = "/hello", produces = {"text/html;charset=UTF-8"})
    public Flux<String> hello(String msg,
                              @RequestParam(defaultValue = "session001") String sessionId) {
        return weatherChatClient.prompt(msg).advisors(
                a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(TimeoutException.class, e -> Flux.just("请求超时，请稍后重试"));
    }
}
