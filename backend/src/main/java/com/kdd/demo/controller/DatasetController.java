package com.kdd.demo.controller;

/**
 * 文件作用：提供数据集读取和 CSV 上传接口。
 * 项目位置：Controller 层，是前端“载入数据/上传 CSV”按钮访问后端数据的统一入口。
 * 交互关系：具体解析 CSV、读取 MySQL、保存上传数据的工作都交给 DatasetService，控制器只负责把结果包装成 JSON。
 */
import com.kdd.demo.service.AlgorithmService;
import com.kdd.demo.service.DatasetService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AlgorithmService algorithmService;

    /**
     * 数据访问集中放在 DatasetService 中，控制器只负责把 HTTP 请求
     * 转换成简单的响应 Map。
     */
    public DatasetController(DatasetService datasetService, AlgorithmService algorithmService) {
        this.datasetService = datasetService;
        this.algorithmService = algorithmService;
    }

    /**
     * 返回用于聚类或分类的 Iris 数据行。
     */
    @GetMapping("/api/iris")
    public Map<String, Object> iris(@RequestParam(value = "dataset", defaultValue = "clustering") String dataset) {
        List<Map<String, Object>> rows;
        if ("classification".equalsIgnoreCase(dataset)) {
            rows = datasetService.getClassificationIrisRows();
        } else {
            rows = datasetService.getClusteringIrisRows();
            return algorithmService.pcaRows(rows);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @PostMapping("/api/iris/pca")
    public Map<String, Object> pca(@RequestBody Map<String, Object> body) {
        Object rows = body.get("rows");
        if (rows instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typedRows = (List<Map<String, Object>>) rows;
            return algorithmService.pcaRows(typedRows);
        }
        return algorithmService.pcaRows(datasetService.getClusteringIrisRows());
    }

    /**
     * 上传 Iris CSV 文件并写入 iris 表。
     */
    @PostMapping("/api/iris/upload")
    public Map<String, Object> uploadIris(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dataset", defaultValue = "clustering") String dataset) throws IOException {
        List<Map<String, Object>> rows = datasetService.uploadIris(file);
        if (!"classification".equalsIgnoreCase(dataset)) {
            return algorithmService.pcaRows(rows);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    /**
     * 返回固定的回归样例数据。
     */
    @GetMapping("/api/regression/data")
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
    @GetMapping("/api/transactions")
    public Map<String, Object> transactions() {
        List<List<String>> transactions = datasetService.getTransactions();
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }

    /**
     * 上传事务 CSV 并写入 transaction_items 表。
     */
    @PostMapping("/api/transactions/upload")
    public Map<String, Object> uploadTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        List<List<String>> transactions = datasetService.uploadTransactions(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }
}
