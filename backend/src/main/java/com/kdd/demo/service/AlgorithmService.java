package com.kdd.demo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlgorithmService {
    private final DatasetService datasetService;
    private final PythonAlgorithmService pythonAlgorithmService;

    public AlgorithmService(DatasetService datasetService, PythonAlgorithmService pythonAlgorithmService) {
        this.datasetService = datasetService;
        this.pythonAlgorithmService = pythonAlgorithmService;
    }

    public Map<String, Object> association(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactions", datasetService.getTransactions());
        payload.put("min_support", number(body, "min_support", 0.2));
        payload.put("min_confidence", number(body, "min_confidence", 0.4));
        return pythonAlgorithmService.run("association", payload);
    }

    public Map<String, Object> clustering(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", datasetService.getClusteringIrisRows());
        payload.put("k", (int) number(body, "k", 3));
        return pythonAlgorithmService.run("clustering", payload);
    }

    public Map<String, Object> classification(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", datasetService.getClassificationIrisRows());
        payload.put("max_depth", (int) number(body, "max_depth", 3));
        payload.put("min_leaf", (int) number(body, "min_leaf", 2));
        return pythonAlgorithmService.run("classification", payload);
    }

    public Map<String, Object> regression(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        Object rows = body.get("rows");
        if (rows instanceof List) {
            payload.put("rows", rows);
        } else {
            payload.put("rows", datasetService.getRegressionRows());
        }
        payload.put("x_field", string(body, "x_field", "x"));
        payload.put("y_field", string(body, "y_field", "y"));
        return pythonAlgorithmService.run("regression", payload);
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
