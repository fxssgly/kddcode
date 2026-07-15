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

    public RegressionController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @PostMapping({"/api/regression", "/api/analysis/regression"})
    public Map<String, Object> regression(@RequestBody Map<String, Object> body) {
        return algorithmService.regression(body);
    }
}
