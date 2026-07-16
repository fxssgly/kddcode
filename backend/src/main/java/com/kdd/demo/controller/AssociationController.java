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
public class AssociationController {
    private final AlgorithmService algorithmService;

    /**
     * 注入算法服务层，由服务层准备关联规则输入数据，
     * 并把真正的挖掘计算交给 Python 算法进程执行。
     */
    public AssociationController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * 执行关联规则挖掘。
     *
     * 请求参数可包含 min_support 和 min_confidence。
     * 这里保留两个访问路径，用来兼容不同版本的前端。
     */
    @PostMapping({"/api/association", "/api/analysis/association"})
    public Map<String, Object> association(@RequestBody Map<String, Object> body) {
        return algorithmService.association(body);
    }
}
