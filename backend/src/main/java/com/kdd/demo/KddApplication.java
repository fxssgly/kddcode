package com.kdd.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KddApplication {
    /**
     * 应用程序入口。Spring Boot 会启动内嵌 Web 服务器，
     * 扫描 com.kdd.demo 包下的组件，并完成控制器和服务的装配。
     */
    public static void main(String[] args) {
        SpringApplication.run(KddApplication.class, args);
    }
}
