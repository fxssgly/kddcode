package com.kdd.demo.service;

/**
 * 文件作用：数据集服务层，统一负责 Iris、事务数据和回归数据的读取、上传解析和格式转换。
 * 项目位置：Service 层，连接 Controller、Repository 和 Python 算法输入。
 * 交互关系：DatasetController 调用这里返回给前端；AlgorithmService 调用这里组装算法 payload。
 */
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
    // 三个 Repository 分别对应 iris、regression_data、transaction_items 三张表。
    private final IrisRepository irisRepository;
    private final RegressionRepository regressionRepository;
    private final TransactionItemRepository transactionRepository;
    // 兼容中文 CSV：上传文件可能是 UTF-8，也可能是 GBK。
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

    // 分类和聚类当前共用 Iris 数据，只是在前端和算法侧的使用方式不同。
    public List<Map<String, Object>> getClassificationIrisRows() {
        return getIrisRows();
    }

    // 回归数据按 id 升序返回，保证表格和图表每次展示顺序稳定。
    public List<Map<String, Object>> getRegressionRows() {
        return regressionRepository.findAllByOrderByIdAsc().stream()
                .map(this::toRegressionMap)
                .collect(Collectors.toList());
    }

    // Entity 不能直接返回给前端；这里统一转成 snake_case 字段的 Map，和前端列名保持一致。
    private List<Map<String, Object>> getIrisRows() {
        return irisRepository.findAll().stream()
                .sorted(Comparator.comparing(IrisSample::getId))
                .map(this::toIrisMap)
                .collect(Collectors.toList());
    }

    // transaction_items 是“一行一个商品项”；关联规则算法需要“一笔交易一个商品列表”，所以这里按 transactionId 分组。
    public List<List<String>> getTransactions() {
        List<TransactionItem> rows = transactionRepository.findAllByOrderByTransactionIdAscItemNameAsc();
        Map<Integer, List<String>> grouped = new TreeMap<>();
        for (TransactionItem row : rows) {
            grouped.computeIfAbsent(row.getTransactionId(), key -> new ArrayList<>())
                    .add(repairText(row.getItemName()));
        }
        return new ArrayList<>(grouped.values());
    }

    // 上传 Iris CSV 后覆盖 iris 表，再重新读取数据库，保证返回内容和数据库最终状态一致。
    public List<Map<String, Object>> uploadIris(MultipartFile file) throws IOException {
        List<IrisSample> samples = parseIris(file).stream()
                .map(this::toIrisSample)
                .collect(Collectors.toList());
        irisRepository.deleteAll();
        irisRepository.saveAll(samples);
        return getIrisRows();
    }

    // 上传事务 CSV 后把每个商品项拆成一行保存，方便数据库按 transaction_id 查询和排序。
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

    // 数据库实体字段是 Java 驼峰名，前端和 Python 使用 snake_case，所以在这里做字段转换。
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

    // 上传 CSV 解析出的 Map 要重新组装成 JPA 实体，Repository 才能保存到数据库。
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

    // 回归实体转前端字段，并修复可能出现的中文编码问题。
    private Map<String, Object> toRegressionMap(RegressionSample sample) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", sample.getId());
        row.put("x", sample.getX());
        row.put("y", sample.getY());
        row.put("type", repairText(sample.getType()));
        return row;
    }

    // Iris CSV 解析入口：先读文本行，再按表头映射出统一字段。
    private List<Map<String, Object>> parseIris(MultipartFile file) throws IOException {
        return parseIrisLines(readTextLines(file.getBytes()));
    }

    // 事务上传兼容两类文件：事务篮子 CSV，或 Iris CSV 转换出来的离散事务。
    private List<List<String>> parseTransactions(MultipartFile file) throws IOException {
        List<String> lines = readTextLines(file.getBytes());
        if (!lines.isEmpty() && lines.get(0).toLowerCase().contains("species")) {
            return irisRowsToTransactions(parseIrisLines(lines));
        }
        return parseTransactionLines(lines);
    }

    // 根据 CSV 表头识别字段，允许常见英文列名和缩写，降低上传文件格式要求。
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

    // 普通事务 CSV 默认第一列是交易编号，后面的列是商品项。
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

    // 把 Iris 连续数值离散成 low/medium/high，转换为关联规则算法能处理的事务项。
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

    // 用两个阈值把连续数值分成 low、medium、high 三档。
    private String level(double value, double low, double high) {
        if (value < low) {
            return "low";
        }
        if (value > high) {
            return "high";
        }
        return "medium";
    }

    // 从多个可能列名中读取数字；都读不到时使用默认值。
    private double numberValue(Map<String, String> raw, double defaultValue, String... names) {
        for (String name : names) {
            String value = raw.get(name);
            if (value != null && !value.isBlank()) {
                return Double.parseDouble(value);
            }
        }
        return defaultValue;
    }

    // 从多个可能列名中读取文本；读取后顺手做编码修复。
    private String stringValue(Map<String, String> raw, String defaultValue, String... names) {
        for (String name : names) {
            String value = raw.get(name);
            if (value != null && !value.isBlank()) {
                return repairText(value);
            }
        }
        return defaultValue;
    }

    // 同时尝试 UTF-8 和 GBK，选择乱码得分更低的文本作为最终内容。
    private List<String> readTextLines(byte[] bytes) {
        String utf8Text = new String(bytes, StandardCharsets.UTF_8);
        String gbkText = new String(bytes, gbk);
        String text = scoreText(gbkText) < scoreText(utf8Text) ? gbkText : utf8Text;
        return Arrays.stream(repairText(text).split("\\R"))
                .collect(Collectors.toList());
    }

    // 修复“UTF-8 被误按 GBK 解码”一类常见中文乱码。
    private String repairText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String repaired = repairGbkDecodedUtf8(value);
        return scoreText(repaired) < scoreText(value) ? repaired : value;
    }

    // 尝试把疑似乱码文本按 GBK 字节重新解释为 UTF-8。
    private String repairGbkDecodedUtf8(String value) {
        try {
            return new String(value.getBytes(gbk), StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    // 给乱码特征打分；分数越高，越可能不是正确编码。
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

    // 当前项目 CSV 只需要处理逗号分隔和 BOM，不支持复杂引号嵌套。
    private String[] splitCsvLine(String line) {
        return line.replace("\uFEFF", "").split("\\s*,\\s*");
    }
}
