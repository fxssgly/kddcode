package com.kdd.demo.controller;

/**
 * 文件作用：暴露“关联规则分析”的 HTTP 接口。
 * 项目位置：Controller 层，直接接收 Vue 前端通过 Axios 发来的 /api/association 请求。
 * 交互关系：本类不做算法计算，只把请求参数交给 AlgorithmService；AlgorithmService 再读取事务数据并调用 Python。
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
     */
    @PostMapping("/api/association")
    public Map<String, Object> association(@RequestBody Map<String, Object> body) {
        return algorithmService.association(body);
    }
}
