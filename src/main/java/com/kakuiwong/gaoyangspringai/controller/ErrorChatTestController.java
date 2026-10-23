package com.kakuiwong.gaoyangspringai.controller;

import com.kakuiwong.gaoyangspringai.ai.tools.ComputerTool;
import com.kakuiwong.gaoyangspringai.ai.tools.WetherErrorTool;
import com.kakuiwong.gaoyangspringai.service.OriginChatTestService;
import com.kakuiwong.gaoyangspringai.util.ThreadPoolUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Duration;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class ErrorChatTestController {

    @Autowired
    OllamaChatModel chatModel;
    @Autowired
    WetherErrorTool errorTool;
    @Autowired
    ComputerTool computerTool;


    @RequestMapping(value = "/test/error", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter originHello(String msg,
                                  @RequestParam(defaultValue = "session001") String sessionId) {

        SseEmitter emitter = new SseEmitter(OriginChatTestService.SSE_TIMEOUT_MILLISECOND);

        ThreadPoolUtil.getPool().execute(() -> {
            ChatClient client = ChatClient.builder(chatModel)
                    .defaultTools(errorTool, computerTool)
                    .build();

            Disposable subscribe = null;
            try {
                subscribe = client.prompt()
                        //.system("")
                        .user(msg)
                        .stream()
                        .chatResponse()
                        .timeout(Duration.ofMinutes(2))
                        .doOnNext(chatResponse -> {
                            String content = chatResponse.getResult().getOutput().getText();
                            try {
                                emitter.send(SseEmitter.event().data(content));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(
                                () -> {
                                    emitter.complete();
                                })
                        .doOnError(e -> emitter.completeWithError(e))
                        .doFinally(signal -> {
                        })
                        .subscribe();
            } catch (Exception e) {
                try {
                    subscribe.dispose();
                    emitter.send(SseEmitter.event().data("发生错误->" + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                }
            }
        });

        return emitter;
    }
}
