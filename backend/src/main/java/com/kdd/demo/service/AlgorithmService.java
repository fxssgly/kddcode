package com.kdd.demo.service;

/**
 * 文件作用：算法请求编排层，负责把控制器请求转换成 Python 算法需要的 payload。
 * 项目位置：Service 层，夹在 Controller 和 PythonAlgorithmService/DatasetService 之间。
 * 交互关系：先从 DatasetService 拿数据，再调用 PythonAlgorithmService.run；相当于“准备食材并下单给算法厨房”。
 */
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    public Map<String, Object> pcaRows(List<Map<String, Object>> rows) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", rows);
        try {
            return pythonAlgorithmService.run("pca", payload);
        } catch (RuntimeException ex) {
            return pcaRowsFallback(rows);
        }
    }

    public Map<String, Object> classification(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", datasetService.getClassificationIrisRows());
        payload.put("max_depth", (int) number(body, "max_depth", 3));
        payload.put("min_leaf", (int) number(body, "min_leaf", 2));
        return pythonAlgorithmService.run("classification", payload);
    }

    /**
     * 使用固定回归样例数据执行回归分析。
     */
    public Map<String, Object> regression(Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", datasetService.getRegressionRows());
        payload.put("x_field", string(body, "x_field", "x"));
        payload.put("y_field", string(body, "y_field", "y"));
        return pythonAlgorithmService.run("regression", payload);
    }

    /**
     * 从 JSON 中读取数值参数。
     * 前端可能发送真正的 JSON 数字，也可能发送表单字符串，所以这里同时支持两种形式。
     */
    private Map<String, Object> pcaRowsFallback(List<Map<String, Object>> sourceRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : sourceRows) {
            rows.add(new LinkedHashMap<>(row));
        }
        double[][] matrix = standardize(rows);
        double[][] covariance = covariance(matrix);
        if (covariance.length > 0) {
            double[] firstVector = powerVector(covariance, null);
            double firstValue = eigenvalue(covariance, firstVector);
            double[][] deflated = deflate(covariance, firstValue, firstVector);
            double[] secondVector = powerVector(deflated, new double[] {0.5, -1.0, 0.75, -0.25});
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                rows.get(rowIndex).put("pca1", round4(dot(matrix[rowIndex], firstVector)));
                rows.get(rowIndex).put("pca2", round4(dot(matrix[rowIndex], secondVector)));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    private double[][] standardize(List<Map<String, Object>> rows) {
        List<String> features = Arrays.asList("sepal_length", "sepal_width", "petal_length", "petal_width");
        double[][] matrix = new double[rows.size()][features.size()];
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            for (int colIndex = 0; colIndex < features.size(); colIndex++) {
                matrix[rowIndex][colIndex] = number(rows.get(rowIndex).get(features.get(colIndex)), 0);
            }
        }
        if (rows.isEmpty()) {
            return matrix;
        }
        for (int colIndex = 0; colIndex < features.size(); colIndex++) {
            double mean = 0.0;
            for (double[] row : matrix) {
                mean += row[colIndex];
            }
            mean /= rows.size();
            double variance = 0.0;
            for (double[] row : matrix) {
                variance += Math.pow(row[colIndex] - mean, 2);
            }
            double std = Math.sqrt(variance / rows.size());
            if (std == 0.0) {
                std = 1.0;
            }
            for (double[] row : matrix) {
                row[colIndex] = (row[colIndex] - mean) / std;
            }
        }
        return matrix;
    }

    private double[][] covariance(double[][] matrix) {
        if (matrix.length == 0) {
            return new double[0][0];
        }
        int columns = matrix[0].length;
        double denominator = Math.max(matrix.length - 1.0, 1.0);
        double[][] covariance = new double[columns][columns];
        for (int rowIndex = 0; rowIndex < columns; rowIndex++) {
            for (int colIndex = 0; colIndex < columns; colIndex++) {
                double sum = 0.0;
                for (double[] row : matrix) {
                    sum += row[rowIndex] * row[colIndex];
                }
                covariance[rowIndex][colIndex] = sum / denominator;
            }
        }
        return covariance;
    }

    private double[] powerVector(double[][] matrix, double[] initial) {
        double[] vector = initial == null ? new double[matrix.length] : initial.clone();
        if (initial == null) {
            Arrays.fill(vector, 1.0);
        }
        normalize(vector);
        for (int iteration = 0; iteration < 80; iteration++) {
            double[] nextVector = multiply(matrix, vector);
            double length = norm(nextVector);
            if (length < 1e-12) {
                break;
            }
            vector = nextVector;
            normalize(vector);
        }
        return vector;
    }

    private double[][] deflate(double[][] matrix, double eigenvalue, double[] vector) {
        double[][] deflated = new double[matrix.length][matrix.length];
        for (int rowIndex = 0; rowIndex < matrix.length; rowIndex++) {
            for (int colIndex = 0; colIndex < matrix.length; colIndex++) {
                deflated[rowIndex][colIndex] = matrix[rowIndex][colIndex] - eigenvalue * vector[rowIndex] * vector[colIndex];
            }
        }
        return deflated;
    }

    private double eigenvalue(double[][] matrix, double[] vector) {
        return dot(vector, multiply(matrix, vector));
    }

    private double[] multiply(double[][] matrix, double[] vector) {
        double[] result = new double[matrix.length];
        for (int rowIndex = 0; rowIndex < matrix.length; rowIndex++) {
            result[rowIndex] = dot(matrix[rowIndex], vector);
        }
        return result;
    }

    private double dot(double[] left, double[] right) {
        double result = 0.0;
        for (int index = 0; index < left.length; index++) {
            result += left[index] * right[index];
        }
        return result;
    }

    private void normalize(double[] vector) {
        double length = norm(vector);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= length;
        }
    }

    private double norm(double[] vector) {
        double length = Math.sqrt(dot(vector, vector));
        return length == 0.0 ? 1.0 : length;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private double number(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
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

    /**
     * 从 JSON 中读取字符串参数；没有传值时使用默认值。
     */
    private String string(Map<String, Object> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value == null ? defaultValue : value.toString();
    }
}
