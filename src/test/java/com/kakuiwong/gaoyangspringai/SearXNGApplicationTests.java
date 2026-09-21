package com.kakuiwong.gaoyangspringai;

import com.kakuiwong.gaoyangspringai.service.WebSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SearXNGApplicationTests {

    @Autowired
    WebSearchService webSearchService;

    @Test
    void contextLoads() {
        String val = webSearchService.search("日本");
        System.out.println(val);
    }

}
