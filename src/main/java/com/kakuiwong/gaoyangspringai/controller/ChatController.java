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
    @Autowired
    ChatClient computerChatClient;

    /* @RequestMapping(value = "/hello", produces = {"text/html;charset=UTF-8"})
     public Flux<String> hello(String msg,
                               @RequestParam(defaultValue = "session001") String sessionId) {
         String result = weatherChatClient.prompt(msg).advisors(
                         a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                 .call()
                 .content();
         return Flux.just(result);
     }
 */
    @RequestMapping(value = "/hello", produces = {"text/html;charset=UTF-8"})
    public Flux<String> hello(String msg,
                              @RequestParam(defaultValue = "session001") String sessionId) {
        return weatherChatClient.prompt(msg).advisors(
                        a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(TimeoutException.class,
                        e -> Flux.just("请求超时，请稍后重试:" + e.getMessage()));
    }

    @RequestMapping(value = "/computer", produces = {"text/html;charset=UTF-8"})
    public Flux<String> computer(String msg,
                              @RequestParam(defaultValue = "session001") String sessionId) {
        return computerChatClient.prompt(msg).advisors(
                        a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(TimeoutException.class,
                        e -> Flux.just("请求超时，请稍后重试:" + e.getMessage()));
    }
}
