package com.kakuiwong.gaoyangspringai.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.kakuiwong.gaoyangspringai.entity.Ceshi;
import com.kakuiwong.gaoyangspringai.service.CeshiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class SqlController {

    @Autowired
    CeshiService ceshiService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RequestMapping(value = "/sql")
    public List<Ceshi> all() {
        return ceshiService.all();
    }


    @GetMapping("/redis")
    public String redis(String key) {
        String value = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(value)) {
            value = key + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            redisTemplate.opsForValue().set(key, value);
        }
        return value;
    }
}
