package com.kakuiwong.gaoyangspringai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakuiwong.gaoyangspringai.entity.Ceshi;
import com.kakuiwong.gaoyangspringai.mapper.CeshiMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: gaoyang
 * @Description:
 */
@Service
public class CeshiService {

    @Resource
    CeshiMapper ceshiMapper;

    public List<Ceshi> all() {
       return ceshiMapper.selectList(new LambdaQueryWrapper<>());
    }
}
