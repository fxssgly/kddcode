package com.kdd.demo.controller;

/**
 * 文件作用：暴露“决策树分类分析”的 HTTP 接口。
 * 项目位置：Controller 层，对应前端 ClassificationView.vue 的 CART 分类按钮。
 * 交互关系：接收 max_depth、min_leaf 等参数，然后交给 AlgorithmService 组织 Iris 数据并调用 Python 分类算法。
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
public class ClassificationController {
    private final AlgorithmService algorithmService;

    /**
     * 注入所有数据挖掘接口共用的算法服务。
     */
    public ClassificationController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * 基于 Iris 数据构建并评估决策树分类器。
     */
    @PostMapping("/api/classification")
    public Map<String, Object> classification(@RequestBody Map<String, Object> body) {
        return algorithmService.classification(body);
    }
}
