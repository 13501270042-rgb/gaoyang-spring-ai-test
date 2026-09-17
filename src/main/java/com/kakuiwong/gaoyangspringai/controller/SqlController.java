package com.kakuiwong.gaoyangspringai.controller;

import com.kakuiwong.gaoyangspringai.entity.Ceshi;
import com.kakuiwong.gaoyangspringai.service.CeshiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: gaoyang
 * @Description:
 */
@RestController
public class SqlController {

    @Autowired
    CeshiService ceshiService;

    @RequestMapping(value = "/sql")
    public List<Ceshi> all() {
        return ceshiService.all();
    }
}
