package com.kakuiwong.gaoyangspringai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:database.xml")
public class GaoyangSpringAiApplication {


    public static void main(String[] args) {
        SpringApplication.run(GaoyangSpringAiApplication.class, args);
    }

}
