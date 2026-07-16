package com.kdd.demo.service;

import com.kdd.demo.entity.IrisSample;
import com.kdd.demo.entity.TransactionItem;
import com.kdd.demo.repository.IrisRepository;
import com.kdd.demo.repository.TransactionItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DatasetService {
    private final IrisRepository irisRepository;
    private final TransactionItemRepository transactionRepository;
    private final boolean useMysql;
    private final Charset gbk = Charset.forName("GBK");
    private List<Map<String, Object>> uploadedIrisRows;
    private List<List<String>> uploadedTransactions;

    public DatasetService(
            IrisRepository irisRepository,
            TransactionItemRepository transactionRepository,
            @Value("${kdd.use-mysql:false}") boolean useMysql) {
        this.irisRepository = irisRepository;
        this.transactionRepository = transactionRepository;
        this.useMysql = useMysql;
    }

    public List<Map<String, Object>> getClusteringIrisRows() {
        return getIrisRows(Paths.get("data", "iris.csv"));
    }

    public List<Map<String, Object>> getClassificationIrisRows() {
        return getIrisRows(Paths.get("data", "iris2.csv"));
    }

    public List<Map<String, Object>> getRegressionRows() {
        return readRegressionCsv(Paths.get("data", "regression_experiment.csv"));
    }

    private List<Map<String, Object>> getIrisRows(Path csvPath) {
        if (uploadedIrisRows != null) {
            return uploadedIrisRows;
        }
        if (useMysql) {
            try {
                List<IrisSample> samples = irisRepository.findAll();
                if (!samples.isEmpty()) {
                    return samples.stream()
                            .sorted(Comparator.comparing(IrisSample::getId))
                            .map(this::toIrisMap)
                            .collect(Collectors.toList());
                }
            } catch (RuntimeException ignored) {
                // MySQL is optional for classroom demos. Fall back to CSV when it is unavailable.
            }
        }
        return readIrisCsv(csvPath);
    }

    public List<List<String>> getTransactions() {
        if (uploadedTransactions != null) {
            return uploadedTransactions;
        }
        if (useMysql) {
            try {
                List<TransactionItem> rows = transactionRepository.findAllByOrderByTransactionIdAscItemNameAsc();
                if (!rows.isEmpty()) {
                    Map<Integer, List<String>> grouped = new TreeMap<>();
                    for (TransactionItem row : rows) {
                        grouped.computeIfAbsent(row.getTransactionId(), key -> new ArrayList<>()).add(repairText(row.getItemName()));
                    }
                    return new ArrayList<>(grouped.values());
                }
            } catch (RuntimeException ignored) {
                // MySQL is optional for classroom demos. Fall back to CSV when it is unavailable.
            }
        }
        return readTransactionsCsv(Paths.get("data", "transactions_sample.csv"));
    }

    public List<Map<String, Object>> uploadIris(MultipartFile file) throws IOException {
        uploadedIrisRows = parseIris(file);
        return uploadedIrisRows;
    }

    public List<List<String>> uploadTransactions(MultipartFile file) throws IOException {
        uploadedTransactions = parseTransactions(file);
        return uploadedTransactions;
    }

    public List<Map<String, Object>> uploadRegression(MultipartFile file) throws IOException {
        return parseRegressionLines(readTextLines(file.getBytes()));
    }

    private Map<String, Object> toIrisMap(IrisSample sample) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", sample.getId());
        row.put("sepal_length", sample.getSepalLength());
        row.put("sepal_width", sample.getSepalWidth());
        row.put("petal_length", sample.getPetalLength());
        row.put("petal_width", sample.getPetalWidth());
        row.put("species", sample.getSpecies());
        return row;
    }

    private List<Map<String, Object>> readIrisCsv(Path path) {
        try {
            return parseIrisLines(readTextLines(Files.readAllBytes(path)));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read iris CSV: " + path.toAbsolutePath(), ex);
        }
    }

    private List<List<String>> readTransactionsCsv(Path path) {
        try {
            return parseTransactionLines(readTextLines(Files.readAllBytes(path)));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read transactions CSV: " + path.toAbsolutePath(), ex);
        }
    }

    private List<Map<String, Object>> readRegressionCsv(Path path) {
        try {
            return parseRegressionLines(readTextLines(Files.readAllBytes(resolveDataPath(path))));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read regression CSV: " + path.toAbsolutePath(), ex);
        }
    }

    private Path resolveDataPath(Path path) {
        if (Files.exists(path)) {
            return path;
        }
        Path backendPath = Paths.get("backend").resolve(path);
        if (Files.exists(backendPath)) {
            return backendPath;
        }
        return path;
    }

    private List<Map<String, Object>> parseIris(MultipartFile file) throws IOException {
        return parseIrisLines(readTextLines(file.getBytes()));
    }

    private List<List<String>> parseTransactions(MultipartFile file) throws IOException {
        List<String> lines = readTextLines(file.getBytes());
        if (!lines.isEmpty() && lines.get(0).toLowerCase().contains("species")) {
            return irisRowsToTransactions(parseIrisLines(lines));
        }
        return parseTransactionLines(lines);
    }

    private List<Map<String, Object>> parseIrisLines(List<String> lines) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (lines.isEmpty()) {
            return rows;
        }
        String[] headers = splitCsvLine(lines.get(0));
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).trim().isEmpty()) {
                continue;
            }
            String[] values = splitCsvLine(lines.get(index));
            Map<String, String> raw = new LinkedHashMap<>();
            for (int i = 0; i < headers.length && i < values.length; i++) {
                raw.put(headers[i].trim(), values[i].trim());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", (int) numberValue(raw, index, "id", "ID"));
            row.put("sepal_length", numberValue(raw, 0, "sepal_length", "Sepal.Length", "SepL", "SepalLength"));
            row.put("sepal_width", numberValue(raw, 0, "sepal_width", "Sepal.Width", "SepW", "SepalWidth"));
            row.put("petal_length", numberValue(raw, 0, "petal_length", "Petal.Length", "PetL", "PetalLength"));
            row.put("petal_width", numberValue(raw, 0, "petal_width", "Petal.Width", "PetW", "PetalWidth"));
            row.put("species", stringValue(raw, "unknown", "species", "Species"));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> parseRegressionLines(List<String> lines) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (lines.isEmpty()) {
            return rows;
        }
        String[] headers = splitCsvLine(lines.get(0));
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).trim().isEmpty()) {
                continue;
            }
            String[] values = splitCsvLine(lines.get(index));
            Map<String, String> raw = new LinkedHashMap<>();
            for (int i = 0; i < headers.length && i < values.length; i++) {
                raw.put(headers[i].trim(), values[i].trim());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", (int) numberValue(raw, index, "id", "ID"));
            row.put("x", numberValue(raw, 0, "x", "X"));
            row.put("y", numberValue(raw, 0, "y", "Y"));
            row.put("type", stringValue(raw, "正常点", "type", "Type"));
            rows.add(row);
        }
        return rows;
    }

    private List<List<String>> parseTransactionLines(List<String> lines) {
        List<List<String>> transactions = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).trim().isEmpty()) {
                continue;
            }
            String[] parts = splitCsvLine(lines.get(index));
            if (index == 0 && parts.length > 0 && ("id".equalsIgnoreCase(parts[0]) || "transaction_id".equalsIgnoreCase(parts[0]))) {
                continue;
            }
            List<String> items = Arrays.stream(parts)
                    .skip(1)
                    .map(String::trim)
                    .map(this::repairText)
                    .filter(value -> !value.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!items.isEmpty()) {
                transactions.add(items);
            }
        }
        return transactions;
    }

    private List<List<String>> irisRowsToTransactions(List<Map<String, Object>> rows) {
        List<List<String>> transactions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            transactions.add(Arrays.asList(
                    "species=" + row.get("species"),
                    "sepal_length=" + level((double) row.get("sepal_length"), 5.5, 6.5),
                    "sepal_width=" + level((double) row.get("sepal_width"), 2.8, 3.4),
                    "petal_length=" + level((double) row.get("petal_length"), 2.0, 5.0),
                    "petal_width=" + level((double) row.get("petal_width"), 0.8, 1.8)
            ));
        }
        return transactions;
    }

    private String level(double value, double low, double high) {
        if (value < low) {
            return "low";
        }
        if (value > high) {
            return "high";
        }
        return "medium";
    }

    private double numberValue(Map<String, String> raw, double defaultValue, String... names) {
        for (String name : names) {
            String value = raw.get(name);
            if (value != null && !value.isBlank()) {
                return Double.parseDouble(value);
            }
        }
        return defaultValue;
    }

    private String stringValue(Map<String, String> raw, String defaultValue, String... names) {
        for (String name : names) {
            String value = raw.get(name);
            if (value != null && !value.isBlank()) {
                return repairText(value);
            }
        }
        return defaultValue;
    }

    private List<String> readTextLines(byte[] bytes) {
        String utf8Text = new String(bytes, StandardCharsets.UTF_8);
        String gbkText = new String(bytes, gbk);
        String text = scoreText(gbkText) < scoreText(utf8Text) ? gbkText : utf8Text;
        return Arrays.stream(repairText(text).split("\\R"))
                .collect(Collectors.toList());
    }

    private String repairText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String repaired = repairGbkDecodedUtf8(value);
        return scoreText(repaired) < scoreText(value) ? repaired : value;
    }

    private String repairGbkDecodedUtf8(String value) {
        try {
            return new String(value.getBytes(gbk), StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private int scoreText(String text) {
        int score = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\uFFFD') {
                score += 10;
            }
        }
        String[] mojibakeTokens = {"锟", "Ã", "Â", "å", "æ", "ç", "闈", "鍏", "鏁", "绫", "灏", "瀹", "搴", "鐑"};
        for (String token : mojibakeTokens) {
            if (text.contains(token)) {
                score += 5;
            }
        }
        return score;
    }

    private String[] splitCsvLine(String line) {
        return line.replace("\uFEFF", "").split("\\s*,\\s*");
    }
}
