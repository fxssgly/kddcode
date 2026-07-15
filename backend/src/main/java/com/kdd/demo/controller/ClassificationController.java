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
public class ClassificationController {
    private final AlgorithmService algorithmService;

    public ClassificationController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @PostMapping({"/api/classification", "/api/analysis/classification"})
    public Map<String, Object> classification(@RequestBody Map<String, Object> body) {
        return algorithmService.classification(body);
    }
}
