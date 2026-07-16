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
public class RegressionController {
    private final AlgorithmService algorithmService;

    /**
     * 注入共用算法服务，用于选择数据并调用 Python 算法。
     */
    public RegressionController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * 对上传数据或默认数据执行线性、多项式和 RANSAC 回归。
     *
     * 请求体可以直接包含 rows，也可以通过 x_field/y_field 指定自定义列名。
     */
    @PostMapping({"/api/regression", "/api/analysis/regression"})
    public Map<String, Object> regression(@RequestBody Map<String, Object> body) {
        return algorithmService.regression(body);
    }
}
