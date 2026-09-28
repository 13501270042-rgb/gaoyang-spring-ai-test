package com.kakuiwong.gaoyangspringai.controller;

import com.kakuiwong.gaoyangspringai.service.OriginChatTestService;
import com.kakuiwong.gaoyangspringai.util.ThreadPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: gaoyang
 * @Description: 手动整合
 */
@RestController
public class OriginChatTestController {

    @Autowired
    OriginChatTestService originChatTestService;


    @RequestMapping(value = "/origin/hello", produces = {"text/event-stream;charset=UTF-8"})
    public SseEmitter originHello(String msg,
                                  @RequestParam(defaultValue = "session001") String sessionId) {

        SseEmitter emitter = new SseEmitter(OriginChatTestService.SSE_TIMEOUT_MILLISECOND);
        AtomicBoolean isSseEmitterFinsh = new AtomicBoolean(false);
        emitter.onError(e -> {
            System.err.println(" ==========SseEmitter.onError->" + e.getMessage());
            isSseEmitterFinsh.set(true);
        });
        emitter.onTimeout(() -> {
            System.err.println(" ==========SseEmitter.onTimeout->");
            isSseEmitterFinsh.set(true);
        });
        emitter.onCompletion(()->{
            System.err.println(" ==========SseEmitter.onCompletion->");
            isSseEmitterFinsh.set(true);
        });

        ThreadPoolUtil.getPool().submit(() -> {
            originChatTestService.chat(msg, sessionId, emitter, isSseEmitterFinsh);
        });

        return emitter;
    }
}
