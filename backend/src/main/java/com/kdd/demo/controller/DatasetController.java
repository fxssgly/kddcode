package com.kdd.demo.controller;

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

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping({"/api/iris", "/api/datasets/iris"})
    public Map<String, Object> iris(@RequestParam(value = "dataset", defaultValue = "default") String dataset) {
        List<Map<String, Object>> rows;
        if ("clustering".equalsIgnoreCase(dataset)) {
            rows = datasetService.getClusteringIrisRows();
        } else if ("classification".equalsIgnoreCase(dataset)) {
            rows = datasetService.getClassificationIrisRows();
        } else {
            rows = datasetService.getIrisRows();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @PostMapping({"/api/iris/upload", "/api/datasets/iris/upload"})
    public Map<String, Object> uploadIris(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = datasetService.uploadIris(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @PostMapping({"/api/regression/upload", "/api/datasets/regression/upload"})
    public Map<String, Object> uploadRegression(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = datasetService.uploadRegression(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @GetMapping({"/api/regression/data", "/api/datasets/regression"})
    public Map<String, Object> regression() {
        List<Map<String, Object>> rows = datasetService.getRegressionRows();
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @GetMapping({"/api/transactions", "/api/datasets/transactions"})
    public Map<String, Object> transactions() {
        List<List<String>> transactions = datasetService.getTransactions();
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }

    @PostMapping({"/api/transactions/upload", "/api/datasets/transactions/upload"})
    public Map<String, Object> uploadTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        List<List<String>> transactions = datasetService.uploadTransactions(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }
}
