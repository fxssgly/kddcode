package com.kdd.demo.controller;

/**
 * 文件作用：提供数据集读取和 CSV 上传接口。
 * 项目位置：Controller 层，是前端“载入数据/上传 CSV”按钮访问后端数据的统一入口。
 * 交互关系：具体解析 CSV、读取 MySQL、保存上传数据的工作都交给 DatasetService，控制器只负责把结果包装成 JSON。
 */
import com.kdd.demo.service.DatasetService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://127.0.0.1:8080",
        "http://localhost:8080",
        "http://127.0.0.1:5173",
        "http://localhost:5173"
})
public class DatasetController {
    private final DatasetService datasetService;

    /**
     * 数据访问集中放在 DatasetService 中，控制器只负责把 HTTP 请求
     * 转换成简单的响应 Map。
     */
    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /**
     * 返回用于聚类或分类的 Iris 数据行。
     *
     * dataset=classification 时返回分类数据集，否则返回聚类数据集。
     * 响应结构保持一致：total + rows。
     */
    @GetMapping({"/api/iris", "/api/datasets/iris"})
    public Map<String, Object> iris(@RequestParam(value = "dataset", defaultValue = "clustering") String dataset) {
        List<Map<String, Object>> rows;
        if ("classification".equalsIgnoreCase(dataset)) {
            rows = datasetService.getClassificationIrisRows();
        } else {
            rows = datasetService.getClusteringIrisRows();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    /**
     * 上传 Iris CSV 文件；启用 MySQL 时会写入 iris 表，数据库不可用时才临时保存在内存中。
     */
    @PostMapping({"/api/iris/upload", "/api/datasets/iris/upload"})
    public Map<String, Object> uploadIris(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = datasetService.uploadIris(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    /**
     * 解析上传的回归 CSV，并返回规范化后的 x/y 数据行。
     */
    @PostMapping({"/api/regression/upload", "/api/datasets/regression/upload"})
    public Map<String, Object> uploadRegression(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = datasetService.uploadRegression(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    /**
     * 返回默认的回归样例数据；启用 MySQL 时优先读取 regression_data 表。
     */
    @GetMapping({"/api/regression/data", "/api/datasets/regression"})
    public Map<String, Object> regression() {
        List<Map<String, Object>> rows = datasetService.getRegressionRows();
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    /**
     * 返回关联规则挖掘使用的事务篮子数据。
     */
    @GetMapping({"/api/transactions", "/api/datasets/transactions"})
    public Map<String, Object> transactions() {
        List<List<String>> transactions = datasetService.getTransactions();
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }

    /**
     * 上传事务 CSV 并写入 transaction_items 表。如果上传文件看起来像 Iris 数据，
     * 服务层会把每一行转换成离散化的事务项。
     */
    @PostMapping({"/api/transactions/upload", "/api/datasets/transactions/upload"})
    public Map<String, Object> uploadTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        List<List<String>> transactions = datasetService.uploadTransactions(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }
}
