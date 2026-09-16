package com.kakuiwong.gaoyangspringai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class HelloController {

    @Autowired
    ChatClient chatClient;

    @RequestMapping("/hello")
    public Flux<String> hello(String msg,
                              @RequestParam(defaultValue = "session001") String cid) {
        return chatClient.prompt(msg).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .stream()
                .content();
    }
}
