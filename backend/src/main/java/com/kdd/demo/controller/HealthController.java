package com.kdd.demo.controller;

/**
 * 文件作用：提供后端根路径和健康检查接口。
 * 项目位置：Controller 层，用于确认 Spring Boot 服务已经启动并可被前端访问。
 * 交互关系：不依赖数据库或算法模块，适合部署或调试时快速判断后端是否在线。
 */
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://127.0.0.1:8080",
        "http://localhost:8080",
        "http://127.0.0.1:5173",
        "http://localhost:5173"
})
public class HealthController {
    /**
     * 根路径接口，用于在浏览器里做一个轻量级启动检查。
     */
    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "KDD Spring Boot backend");
        result.put("status", "running");
        return result;
    }

    /**
     * 面向程序读取的健康检查接口，供前端启动时判断后端状态。
     */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("backend", "spring-boot");
        return result;
    }
}
