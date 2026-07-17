package com.kdd.demo.service;

/**
 * 文件作用：算法请求编排层，负责把控制器请求转换成 Python 算法需要的 payload。
 * 项目位置：Service 层，夹在 Controller 和 PythonAlgorithmService/DatasetService 之间。
 * 交互关系：先从 DatasetService 拿数据，再调用 PythonAlgorithmService.run；相当于“准备食材并下单给算法厨房”。
 */
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlgorithmService {
    private final DatasetService datasetService;
    private final PythonAlgorithmService pythonAlgorithmService;

    /**
     * AlgorithmService 是 HTTP 控制器、本地数据集和 Python 算法实现之间的编排层。
     */
    public AlgorithmService(DatasetService datasetService, PythonAlgorithmService pythonAlgorithmService) {
        this.datasetService = datasetService;
        this.pythonAlgorithmService = pythonAlgorithmService;
    }

    /**
     * 为关联规则挖掘准备事务篮子数据和阈值参数。
     */
    public Map<String, Object> association(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactions", datasetService.getTransactions());
        payload.put("min_support", number(body, "min_support", 0.2));
        payload.put("min_confidence", number(body, "min_confidence", 0.4));
        return pythonAlgorithmService.run("association", payload);
    }

    /**
     * 在聚类用 Iris 数据集上执行指定聚类算法。
     */
    public Map<String, Object> clustering(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        Object rows = body.get("rows");
        if (rows instanceof List) {
            payload.put("rows", rows);
        } else {
            payload.put("rows", datasetService.getClusteringIrisRows());
        }
        payload.put("method", string(body, "method", "kmeans"));
        payload.put("k", (int) number(body, "k", 3));
        payload.put("eps", number(body, "eps", 0.5));
        payload.put("min_samples", (int) number(body, "min_samples", 5));
        payload.put("threshold", number(body, "threshold", 0.5));
        payload.put("bandwidth", number(body, "bandwidth", 0));
        payload.put("linkage", string(body, "linkage", "ward"));
        return pythonAlgorithmService.run("clustering", payload);
    }

    /**
     * 在分类用 Iris 数据集上执行决策树分类。
     */
    public Map<String, Object> classification(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", datasetService.getClassificationIrisRows());
        payload.put("max_depth", (int) number(body, "max_depth", 3));
        payload.put("min_leaf", (int) number(body, "min_leaf", 2));
        return pythonAlgorithmService.run("classification", payload);
    }

    /**
     * 如果客户端提供了 rows，就使用客户端数据做回归；
     * 否则回退到内置的回归样例 CSV。
     */
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

    /**
     * 从 JSON 中读取数值参数。
     * 前端可能发送真正的 JSON 数字，也可能发送表单字符串，所以这里同时支持两种形式。
     */
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

    /**
     * 从 JSON 中读取字符串参数；没有传值时使用默认值。
     */
    private String string(Map<String, Object> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value == null ? defaultValue : value.toString();
    }
}
