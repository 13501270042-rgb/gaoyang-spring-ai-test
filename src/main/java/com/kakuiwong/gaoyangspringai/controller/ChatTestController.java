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
