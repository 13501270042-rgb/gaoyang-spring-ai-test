package com.kakuiwong.gaoyangspringai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: gaoyang
 * @Description:
 */
@Data
@TableName("spring_ai_chat_memory")
public class SpringAiChatMemory {

    private String conversationId;

    private String content;

    private String type;

    private LocalDateTime timestamp;

    private Long sequenceId;
}
