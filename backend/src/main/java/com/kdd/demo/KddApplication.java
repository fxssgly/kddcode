package com.kdd.demo;

/**
 * 文件作用：Spring Boot 后端应用入口。
 * 项目位置：位于 backend/src/main/java 的根包中，是整个 Java 后端启动时最先执行的类。
 * 交互关系：启动后 Spring 会扫描 controller、service、repository、entity 等组件；
 * 前端请求先进入 controller，再经过 service 读取 CSV/MySQL 或调用 Python 算法。
 */
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
