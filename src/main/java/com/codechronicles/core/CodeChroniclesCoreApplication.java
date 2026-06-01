package com.codechronicles.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.codechronicles.core.mapper")
@SpringBootApplication
public class CodeChroniclesCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeChroniclesCoreApplication.class, args);
    }
}
