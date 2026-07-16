package com.kdd.demo.controller;

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
public class ClassificationController {
    private final AlgorithmService algorithmService;

    /**
     * 注入所有数据挖掘接口共用的算法服务。
     */
    public ClassificationController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * 基于 Iris 数据构建并评估一个小型决策树分类器。
     *
     * 请求体可以提供 max_depth 和 min_leaf，用来调整树的复杂度。
     */
    @PostMapping({"/api/classification", "/api/analysis/classification"})
    public Map<String, Object> classification(@RequestBody Map<String, Object> body) {
        return algorithmService.classification(body);
    }
}
