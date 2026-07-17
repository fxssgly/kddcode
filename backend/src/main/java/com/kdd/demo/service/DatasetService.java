package com.kdd.demo.service;

import com.kdd.demo.entity.IrisSample;
import com.kdd.demo.entity.RegressionSample;
import com.kdd.demo.entity.TransactionItem;
import com.kdd.demo.repository.IrisRepository;
import com.kdd.demo.repository.RegressionRepository;
import com.kdd.demo.repository.TransactionItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private final RegressionRepository regressionRepository;
    private final TransactionItemRepository transactionRepository;
    private final Charset gbk = Charset.forName("GBK");

    public DatasetService(
            IrisRepository irisRepository,
            RegressionRepository regressionRepository,
            TransactionItemRepository transactionRepository) {
        this.irisRepository = irisRepository;
        this.regressionRepository = regressionRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Map<String, Object>> getClusteringIrisRows() {
        return getIrisRows();
    }

    public List<Map<String, Object>> getClassificationIrisRows() {
        return getIrisRows();
    }

    public List<Map<String, Object>> getRegressionRows() {
        return regressionRepository.findAllByOrderByIdAsc().stream()
                .map(this::toRegressionMap)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getIrisRows() {
        return irisRepository.findAll().stream()
                .sorted(Comparator.comparing(IrisSample::getId))
                .map(this::toIrisMap)
                .collect(Collectors.toList());
    }

    public List<List<String>> getTransactions() {
        List<TransactionItem> rows = transactionRepository.findAllByOrderByTransactionIdAscItemNameAsc();
        Map<Integer, List<String>> grouped = new TreeMap<>();
        for (TransactionItem row : rows) {
            grouped.computeIfAbsent(row.getTransactionId(), key -> new ArrayList<>())
                    .add(repairText(row.getItemName()));
        }
        return new ArrayList<>(grouped.values());
    }

    public List<Map<String, Object>> uploadIris(MultipartFile file) throws IOException {
        List<IrisSample> samples = parseIris(file).stream()
                .map(this::toIrisSample)
                .collect(Collectors.toList());
        irisRepository.deleteAll();
        irisRepository.saveAll(samples);
        return getIrisRows();
    }

    public List<List<String>> uploadTransactions(MultipartFile file) throws IOException {
        List<List<String>> transactions = parseTransactions(file);
        List<TransactionItem> rows = new ArrayList<>();
        for (int transactionIndex = 0; transactionIndex < transactions.size(); transactionIndex++) {
            for (String item : transactions.get(transactionIndex)) {
                TransactionItem row = new TransactionItem();
                row.setTransactionId(transactionIndex + 1);
                row.setItemName(item);
                rows.add(row);
            }
        }
        transactionRepository.deleteAll();
        transactionRepository.saveAll(rows);
        return getTransactions();
    }

    public List<Map<String, Object>> uploadRegression(MultipartFile file) throws IOException {
        List<RegressionSample> samples = parseRegressionLines(readTextLines(file.getBytes())).stream()
                .map(this::toRegressionSample)
                .collect(Collectors.toList());
        regressionRepository.deleteAll();
        regressionRepository.saveAll(samples);
        return getRegressionRows();
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

    private IrisSample toIrisSample(Map<String, Object> row) {
        IrisSample sample = new IrisSample();
        sample.setId(((Number) row.get("id")).intValue());
        sample.setSepalLength(((Number) row.get("sepal_length")).doubleValue());
        sample.setSepalWidth(((Number) row.get("sepal_width")).doubleValue());
        sample.setPetalLength(((Number) row.get("petal_length")).doubleValue());
        sample.setPetalWidth(((Number) row.get("petal_width")).doubleValue());
        sample.setSpecies(String.valueOf(row.get("species")));
        return sample;
    }

    private Map<String, Object> toRegressionMap(RegressionSample sample) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", sample.getId());
        row.put("x", sample.getX());
        row.put("y", sample.getY());
        row.put("type", repairText(sample.getType()));
        return row;
    }

    private RegressionSample toRegressionSample(Map<String, Object> row) {
        RegressionSample sample = new RegressionSample();
        sample.setId(((Number) row.get("id")).intValue());
        sample.setX(((Number) row.get("x")).doubleValue());
        sample.setY(((Number) row.get("y")).doubleValue());
        sample.setType(String.valueOf(row.get("type")));
        return sample;
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
            row.put("type", stringValue(raw, "normal", "type", "Type"));
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
        String[] mojibakeTokens = {"?", "Ã", "Â", "å", "æ", "ä", "é", "è", "ç"};
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
