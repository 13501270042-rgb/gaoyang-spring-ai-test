package com.kakuiwong.gaoyangspringai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: gaoyang
 * @Description: 请求任务表
 */
@Data
@TableName("request_task")
public class RequestTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer userId;


    private LocalDateTime createTime;
}
