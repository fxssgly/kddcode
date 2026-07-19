package com.kdd.demo.controller;

/**
 * 文件作用：暴露“回归分析”的 HTTP 接口。
 * 项目位置：Controller 层，对应前端 RegressionView.vue 的开始回归操作。
 * 交互关系：把 x/y 字段和可选 rows 交给 AlgorithmService，再由 Python 算法计算线性、多项式和 RANSAC 回归。
 */
import com.kdd.demo.service.AlgorithmService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://127.0.0.1:8080",
        "http://localhost:8080",
        "http://127.0.0.1:5173",
        "http://localhost:5173"
})
public class RegressionController {
    private final AlgorithmService algorithmService;

    /**
     * 注入共用算法服务，用于选择数据并调用 Python 算法。
     */
    public RegressionController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * 对默认数据执行线性、多项式和 RANSAC 回归。
     */
    @PostMapping("/api/regression")
    public Map<String, Object> regression(@RequestBody Map<String, Object> body) {
        return algorithmService.regression(body);
    }
}
