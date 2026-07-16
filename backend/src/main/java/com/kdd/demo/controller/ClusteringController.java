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
public class ClusteringController {
    private final AlgorithmService algorithmService;

    /**
     * 注入共用算法服务。服务层会先组装请求数据，
     * 再调用 Python 里的具体算法实现。
     */
    public ClusteringController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * 对 Iris 的数值特征执行 K-Means 聚类。
     *
     * 请求体可以提供 k；未提供时默认使用 3 个簇。
     */
    @PostMapping({"/api/clustering", "/api/analysis/clustering"})
    public Map<String, Object> clustering(@RequestBody Map<String, Object> body) {
        return algorithmService.clustering(body);
    }
}
