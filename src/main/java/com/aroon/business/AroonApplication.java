package com.aroon.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aroon.business.mapper")
public class AroonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AroonApplication.class, args);
    }

}
