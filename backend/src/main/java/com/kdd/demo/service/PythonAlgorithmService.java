package com.kdd.demo.service;

/**
 * 文件作用：Java 后端调用 Python 算法脚本的桥梁。
 * 项目位置：Service 层，位于 Java 世界和 backend/python 算法世界的交界处。
 * 交互关系：把 Java Map 写成临时 JSON 文件，启动 kdd_algorithms.py，读取 Python 输出的 JSON 再返回给控制器。
 */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PythonAlgorithmService {
    private final ObjectMapper objectMapper;
    private final String pythonCommand;
    private final Path pythonScript;

    /**
     * 保存 JSON 和 Python 调用相关配置。
     * 默认值允许后端在普通开发机上直接运行，无需额外配置 application.properties。
     */
    public PythonAlgorithmService(
            ObjectMapper objectMapper,
            @Value("${kdd.python-command:python}") String pythonCommand,
            @Value("${kdd.python-script:python/kdd_algorithms.py}") String pythonScript) {
        this.objectMapper = objectMapper;
        this.pythonCommand = pythonCommand;
        this.pythonScript = Paths.get(pythonScript);
    }

    /**
     * 执行一个 Python 算法：
     * 先写入临时 JSON 请求文件，再启动 Python 脚本，最后解析脚本输出的 JSON。
     */
    public Map<String, Object> run(String operation, Map<String, Object> payload) {
        try {
            Path input = Files.createTempFile("kdd-input-", ".json");
            Map<String, Object> request = new HashMap<>();
            request.put("operation", operation);
            request.put("payload", payload);
            Files.writeString(input, objectMapper.writeValueAsString(request), StandardCharsets.UTF_8);

            // 用列表形式构造命令，避免 shell 引号转义问题。
            List<String> command = new ArrayList<>(resolvePythonCommand());
            command.add(resolvePythonScript().toString());
            command.add(input.toString());

            Process process = new ProcessBuilder(command)
                    .directory(Paths.get("").toAbsolutePath().toFile())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            Files.deleteIfExists(input);
            if (exitCode != 0) {
                return fallbackOrThrow(operation, payload, "Python algorithm failed: " + output, null);
            }
            return objectMapper.readValue(output, new TypeReference<Map<String, Object>>() {});
        } catch (IOException ex) {
            return fallbackOrThrow(operation, payload, "Cannot call Python algorithm. Check kdd.python-command.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python algorithm was interrupted.", ex);
        }
    }

    /**
     * 聚类算法提供 Java 兜底实现，这样即使缺少 Python，课堂演示仍可运行。
     * 其他算法只在 Python 中实现，因此失败时直接抛出错误。
     */
    private Map<String, Object> fallbackOrThrow(String operation, Map<String, Object> payload, String message, Exception cause) {
        if ("clustering".equals(operation) && "kmeans".equalsIgnoreCase(String.valueOf(payload.getOrDefault("method", "kmeans")))) {
            return clusteringFallback(payload);
        }
        if (cause == null) {
            throw new IllegalStateException(message);
        }
        throw new IllegalStateException(message, cause);
    }

    /**
     * 最小版 K-Means 兜底实现，仅在 Python 无法执行时使用。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> clusteringFallback(Map<String, Object> payload) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : (List<Object>) payload.getOrDefault("rows", new ArrayList<>())) {
            rows.add(new LinkedHashMap<>((Map<String, Object>) item));
        }
        if (rows.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("rows", rows);
            result.put("centers", new ArrayList<>());
            return result;
        }

        int k = Math.max(1, Math.min((int) number(payload.get("k"), 3), rows.size()));
        List<Map<String, Double>> centers = new ArrayList<>();
        for (int index = 0; index < k; index++) {
            centers.add(centerFrom(rows.get(index)));
        }

        // 反复执行样本分配和中心点重算，直到稳定或达到迭代上限。
        for (int iteration = 0; iteration < 20; iteration++) {
            List<List<Map<String, Object>>> groups = new ArrayList<>();
            for (int index = 0; index < k; index++) {
                groups.add(new ArrayList<>());
            }
            for (Map<String, Object> row : rows) {
                int cluster = nearestCenter(row, centers);
                row.put("cluster", cluster);
                groups.get(cluster).add(row);
            }
            List<Map<String, Double>> nextCenters = new ArrayList<>();
            for (int index = 0; index < k; index++) {
                nextCenters.add(groups.get(index).isEmpty() ? centers.get(index) : averageCenter(groups.get(index)));
            }
            if (nextCenters.equals(centers)) {
                break;
            }
            centers = nextCenters;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("centers", centers);
        return result;
    }

    /**
     * 使用欧氏距离为一条 Iris 数据找到最近的聚类中心。
     */
    private int nearestCenter(Map<String, Object> row, List<Map<String, Double>> centers) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index < centers.size(); index++) {
            double distance = distance(row, centers.get(index));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    /**
     * 基于 Iris 的四个数值特征计算欧氏距离。
     */
    private double distance(Map<String, Object> row, Map<String, Double> center) {
        double sum = 0.0;
        for (String feature : clusteringFeatures(row)) {
            double diff = number(row.get(feature), 0) - center.get(feature);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 对每个特征取平均值，重新计算一个簇的中心点。
     */
    private Map<String, Double> averageCenter(List<Map<String, Object>> rows) {
        Map<String, Double> center = new LinkedHashMap<>();
        for (String feature : clusteringFeatures(rows.get(0))) {
            double sum = 0.0;
            for (Map<String, Object> row : rows) {
                sum += number(row.get(feature), 0);
            }
            center.put(feature, sum / rows.size());
        }
        return center;
    }

    /**
     * 使用已有数据行作为 K-Means 的初始中心点。
     */
    private Map<String, Double> centerFrom(Map<String, Object> row) {
        Map<String, Double> center = new LinkedHashMap<>();
        for (String feature : clusteringFeatures(row)) {
            center.put(feature, number(row.get(feature), 0));
        }
        return center;
    }

    private List<String> clusteringFeatures(Map<String, Object> row) {
        if (row.containsKey("pca1") && row.containsKey("pca2")) {
            return Arrays.asList("pca1", "pca2");
        }
        return Arrays.asList("sepal_length", "sepal_width", "petal_length", "petal_width");
    }

    /**
     * 为兜底计算安全地转换数值。
     */
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

    /**
     * 从当前工作目录或 backend/ 目录解析 Python 脚本路径，
     * 让应用从两个位置启动时都能找到脚本。
     */
    private Path resolvePythonScript() {
        if (pythonScript.isAbsolute() || Files.exists(pythonScript)) {
            return pythonScript;
        }
        Path backendScript = Paths.get("backend").resolve(pythonScript);
        if (Files.exists(backendScript)) {
            return backendScript;
        }
        return pythonScript;
    }

    /**
     * 在 Windows 和其他本地环境中选择可用的 Python 3 命令。
     */
    private List<String> resolvePythonCommand() {
        if (!"python".equalsIgnoreCase(pythonCommand)) {
            return Arrays.asList(pythonCommand);
        }
        if (isPython3("python")) {
            return Arrays.asList("python");
        }
        Path python310 = Paths.get(System.getProperty("user.home"), "AppData", "Local", "Programs", "Python", "Python310", "python.exe");
        if (Files.exists(python310)) {
            return Arrays.asList(python310.toString());
        }
        if (isPython3("py", "-3")) {
            return Arrays.asList("py", "-3");
        }
        return Arrays.asList("python");
    }

    /**
     * 检查某个命令是否能解析到 Python 3。
     */
    private boolean isPython3(String... command) {
        try {
            List<String> versionCommand = new ArrayList<>(Arrays.asList(command));
            versionCommand.add("--version");
            Process process = new ProcessBuilder(versionCommand)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            return exitCode == 0 && output.trim().startsWith("Python 3");
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
