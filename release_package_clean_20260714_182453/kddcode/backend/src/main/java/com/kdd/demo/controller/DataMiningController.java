package com.kdd.demo.controller;

import com.kdd.demo.service.DataService;
import com.kdd.demo.service.PythonAlgorithmService;
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
public class DataMiningController {
    private final DataService dataService;
    private final PythonAlgorithmService algorithmService;

    public DataMiningController(DataService dataService, PythonAlgorithmService algorithmService) {
        this.dataService = dataService;
        this.algorithmService = algorithmService;
    }

    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "KDD Spring Boot backend");
        result.put("status", "running");
        return result;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("backend", "spring-boot");
        return result;
    }

    @GetMapping("/api/iris")
    public Map<String, Object> iris(@RequestParam(value = "dataset", defaultValue = "default") String dataset) {
        List<Map<String, Object>> rows;
        if ("clustering".equalsIgnoreCase(dataset)) {
            rows = dataService.getClusteringIrisRows();
        } else if ("classification".equalsIgnoreCase(dataset)) {
            rows = dataService.getClassificationIrisRows();
        } else {
            rows = dataService.getIrisRows();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @PostMapping("/api/iris/upload")
    public Map<String, Object> uploadIris(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = dataService.uploadIris(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @PostMapping("/api/regression/upload")
    public Map<String, Object> uploadRegression(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = dataService.uploadRegression(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    @GetMapping("/api/transactions")
    public Map<String, Object> transactions() {
        List<List<String>> transactions = dataService.getTransactions();
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }

    @PostMapping("/api/transactions/upload")
    public Map<String, Object> uploadTransactions(@RequestParam("file") MultipartFile file) throws IOException {
        List<List<String>> transactions = dataService.uploadTransactions(file);
        Map<String, Object> result = new HashMap<>();
        result.put("total", transactions.size());
        result.put("transactions", transactions);
        return result;
    }

    @PostMapping("/api/association")
    public Map<String, Object> association(@RequestBody Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactions", dataService.getTransactions());
        payload.put("min_support", number(body, "min_support", 0.2));
        payload.put("min_confidence", number(body, "min_confidence", 0.4));
        return algorithmService.run("association", payload);
    }

    @PostMapping("/api/clustering")
    public Map<String, Object> clustering(@RequestBody Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", dataService.getClusteringIrisRows());
        payload.put("k", (int) number(body, "k", 3));
        return algorithmService.run("clustering", payload);
    }

    @PostMapping("/api/classification")
    public Map<String, Object> classification(@RequestBody Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", dataService.getClassificationIrisRows());
        payload.put("max_depth", (int) number(body, "max_depth", 3));
        payload.put("min_leaf", (int) number(body, "min_leaf", 2));
        return algorithmService.run("classification", payload);
    }

    @PostMapping("/api/regression")
    public Map<String, Object> regression(@RequestBody Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        Object rows = body.get("rows");
        if (rows instanceof List) {
            payload.put("rows", rows);
        } else {
            payload.put("rows", dataService.getRegressionRows());
        }
        payload.put("x_field", string(body, "x_field", "x"));
        payload.put("y_field", string(body, "y_field", "y"));
        return algorithmService.run("regression", payload);
    }

    private double number(Map<String, Object> body, String key, double defaultValue) {
        Object value = body.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) {
            return Double.parseDouble(value.toString());
        }
        return defaultValue;
    }

    private String string(Map<String, Object> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value == null ? defaultValue : value.toString();
    }
}
