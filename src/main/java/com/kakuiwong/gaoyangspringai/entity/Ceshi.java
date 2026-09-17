package com.kakuiwong.gaoyangspringai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: gaoyang
 * @Description:
 */
@Data
@TableName("t_ceshi")
public class Ceshi {

    private Integer id;
    private String name;
}
