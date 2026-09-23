package com.kakuiwong.gaoyangspringai.controller;

import com.kakuiwong.gaoyangspringai.service.OriginChatTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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
                                  @RequestParam(defaultValue = "session001") String sessionId) throws IOException {

        return originChatTestService.chat(msg, sessionId);
    }
}
