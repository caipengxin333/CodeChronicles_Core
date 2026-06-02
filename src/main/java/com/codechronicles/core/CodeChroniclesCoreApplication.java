package com.codechronicles.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.codechronicles.core.mapper")
@SpringBootApplication
public class CodeChroniclesCoreApplication {

    /**
     * 后端服务启动入口，同时通过 {@link MapperScan} 扫描 MyBatis Mapper 接口。
     */
    public static void main(String[] args) {
        SpringApplication.run(CodeChroniclesCoreApplication.class, args);
    }
}
