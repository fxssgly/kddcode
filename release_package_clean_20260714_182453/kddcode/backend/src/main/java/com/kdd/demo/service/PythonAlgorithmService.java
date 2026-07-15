package com.kdd.demo.service;

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

    public PythonAlgorithmService(
            ObjectMapper objectMapper,
            @Value("${kdd.python-command:python}") String pythonCommand,
            @Value("${kdd.python-script:python/kdd_algorithms.py}") String pythonScript) {
        this.objectMapper = objectMapper;
        this.pythonCommand = pythonCommand;
        this.pythonScript = Paths.get(pythonScript);
    }

    public Map<String, Object> run(String operation, Map<String, Object> payload) {
        try {
            Path input = Files.createTempFile("kdd-input-", ".json");
            Map<String, Object> request = new HashMap<>();
            request.put("operation", operation);
            request.put("payload", payload);
            Files.writeString(input, objectMapper.writeValueAsString(request), StandardCharsets.UTF_8);

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

    private Map<String, Object> fallbackOrThrow(String operation, Map<String, Object> payload, String message, Exception cause) {
        if ("clustering".equals(operation)) {
            return clusteringFallback(payload);
        }
        if (cause == null) {
            throw new IllegalStateException(message);
        }
        throw new IllegalStateException(message, cause);
    }

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

    private double distance(Map<String, Object> row, Map<String, Double> center) {
        double sum = 0.0;
        for (String feature : Arrays.asList("sepal_length", "sepal_width", "petal_length", "petal_width")) {
            double diff = number(row.get(feature), 0) - center.get(feature);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private Map<String, Double> averageCenter(List<Map<String, Object>> rows) {
        Map<String, Double> center = new LinkedHashMap<>();
        for (String feature : Arrays.asList("sepal_length", "sepal_width", "petal_length", "petal_width")) {
            double sum = 0.0;
            for (Map<String, Object> row : rows) {
                sum += number(row.get(feature), 0);
            }
            center.put(feature, sum / rows.size());
        }
        return center;
    }

    private Map<String, Double> centerFrom(Map<String, Object> row) {
        Map<String, Double> center = new LinkedHashMap<>();
        for (String feature : Arrays.asList("sepal_length", "sepal_width", "petal_length", "petal_width")) {
            center.put(feature, number(row.get(feature), 0));
        }
        return center;
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
