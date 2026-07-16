package com.kdd.demo.service;

/**
 * 文件作用：统一管理实验数据来源，包括内置 CSV、上传 CSV 和可选 MySQL。
 * 项目位置：Service 层，是所有数据集接口和算法服务共同依赖的数据入口。
 * 交互关系：DatasetController 用它返回表格数据；AlgorithmService 用它把数据送进 Python 算法。
 */
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

    /*
     * 上传的数据只保存在当前后端进程的内存中。
     * 这样课堂演示更简单：上传后会立刻影响后续算法调用，
     * 同时不需要额外实现数据库写入逻辑。
     */
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

    /**
     * 返回用于无监督聚类的 Iris 数据行。
     */
    public List<Map<String, Object>> getClusteringIrisRows() {
        return getIrisRows(Paths.get("data", "iris.csv"));
    }

    /**
     * 返回用于有监督分类的 Iris 数据行。
     */
    public List<Map<String, Object>> getClassificationIrisRows() {
        return getIrisRows(Paths.get("data", "iris2.csv"));
    }

    /**
     * 返回默认的回归样例数据行。
     */
    public List<Map<String, Object>> getRegressionRows() {
        return readRegressionCsv(Paths.get("data", "regression_experiment.csv"));
    }

    /**
     * 按优先级选择当前 Iris 数据源：
     * 上传文件、可选 MySQL 表、最后是项目自带 CSV 文件。
     */
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
                // MySQL 在课堂演示中是可选项；不可用时自动回退到 CSV。
            }
        }
        return readIrisCsv(csvPath);
    }

    /**
     * 从上传数据、可选 MySQL 数据或内置样例 CSV 中选择事务篮子。
     * 数据库行会按 transaction_id 分组成一笔笔事务。
     */
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
                // MySQL 在课堂演示中是可选项；不可用时自动回退到 CSV。
            }
        }
        return readTransactionsCsv(Paths.get("data", "transactions_sample.csv"));
    }

    /**
     * 解析并保存上传的 Iris 数据，供后续聚类或分类使用。
     */
    public List<Map<String, Object>> uploadIris(MultipartFile file) throws IOException {
        uploadedIrisRows = parseIris(file);
        return uploadedIrisRows;
    }

    /**
     * 解析并保存上传的事务篮子，供后续关联规则挖掘使用。
     */
    public List<List<String>> uploadTransactions(MultipartFile file) throws IOException {
        uploadedTransactions = parseTransactions(file);
        return uploadedTransactions;
    }

    /**
     * 解析上传的回归数据，但不保存为全局状态；
     * 调用方可以把返回的 rows 直接传给回归接口。
     */
    public List<Map<String, Object>> uploadRegression(MultipartFile file) throws IOException {
        return parseRegressionLines(readTextLines(file.getBytes()));
    }

    /**
     * 把数据库实体转换成 Python 脚本和前端图表都能识别的 JSON 字段名。
     */
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

    /**
     * 读取并解析 Iris CSV 文件；如果发生 IO 错误，会补充文件路径上下文。
     */
    private List<Map<String, Object>> readIrisCsv(Path path) {
        try {
            return parseIrisLines(readTextLines(Files.readAllBytes(path)));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read iris CSV: " + path.toAbsolutePath(), ex);
        }
    }

    /**
     * 读取并解析事务 CSV 文件。
     */
    private List<List<String>> readTransactionsCsv(Path path) {
        try {
            return parseTransactionLines(readTextLines(Files.readAllBytes(path)));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read transactions CSV: " + path.toAbsolutePath(), ex);
        }
    }

    /**
     * 读取并解析回归 CSV 文件。
     * 这里使用 resolveDataPath，是因为后端可能从仓库根目录或 backend 目录启动。
     */
    private List<Map<String, Object>> readRegressionCsv(Path path) {
        try {
            return parseRegressionLines(readTextLines(Files.readAllBytes(resolveDataPath(path))));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read regression CSV: " + path.toAbsolutePath(), ex);
        }
    }

    /**
     * 从当前目录或 backend/ 目录解析数据文件路径。
     */
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

    /**
     * 对上传的 Iris 文件先做文本编码检测和修复，再进行解析。
     */
    private List<Map<String, Object>> parseIris(MultipartFile file) throws IOException {
        return parseIrisLines(readTextLines(file.getBytes()));
    }

    /**
     * 解析上传的事务数据。
     * 如果文件像 Iris 数据，会转换成离散化篮子，方便复用 Iris 数据做关联规则演示。
     */
    private List<List<String>> parseTransactions(MultipartFile file) throws IOException {
        List<String> lines = readTextLines(file.getBytes());
        if (!lines.isEmpty() && lines.get(0).toLowerCase().contains("species")) {
            return irisRowsToTransactions(parseIrisLines(lines));
        }
        return parseTransactionLines(lines);
    }

    /**
     * 把多种常见 Iris CSV 表头命名统一成算法使用的稳定字段结构。
     */
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

    /**
     * 解析回归 CSV 行，并规范化必需的 x/y 字段。
     */
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

    /**
     * 解析篮子格式的 CSV 行。
     * 第一列视为编号，后续非空单元格会变成该事务中的唯一商品项。
     */
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

    /**
     * 把 Iris 的数值测量值转换成离散化的事务项。
     */
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

    /**
     * 使用两个阈值把数值转换为 low/medium/high 三档。
     */
    private String level(double value, double low, double high) {
        if (value < low) {
            return "low";
        }
        if (value > high) {
            return "high";
        }
        return "medium";
    }

    /**
     * 在一组候选列名中寻找第一个存在的数值字段。
     */
    private double numberValue(Map<String, String> raw, double defaultValue, String... names) {
        for (String name : names) {
            String value = raw.get(name);
            if (value != null && !value.isBlank()) {
                return Double.parseDouble(value);
            }
        }
        return defaultValue;
    }

    /**
     * 在一组候选列名中寻找第一个存在的文本字段，
     * 并应用与 CSV 内容相同的编码修复逻辑。
     */
    private String stringValue(Map<String, String> raw, String defaultValue, String... names) {
        for (String name : names) {
            String value = raw.get(name);
            if (value != null && !value.isBlank()) {
                return repairText(value);
            }
        }
        return defaultValue;
    }

    /**
     * 同时按 UTF-8 和 GBK 解码文件字节，然后选择看起来乱码更少的版本。
     * 这能更好地处理 Excel 保存的中文 CSV 文件。
     */
    private List<String> readTextLines(byte[] bytes) {
        String utf8Text = new String(bytes, StandardCharsets.UTF_8);
        String gbkText = new String(bytes, gbk);
        String text = scoreText(gbkText) < scoreText(utf8Text) ? gbkText : utf8Text;
        return Arrays.stream(repairText(text).split("\\R"))
                .collect(Collectors.toList());
    }

    /**
     * 修复 UTF-8 文本被误按 GBK 解码后产生的常见乱码。
     */
    private String repairText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String repaired = repairGbkDecodedUtf8(value);
        return scoreText(repaired) < scoreText(value) ? repaired : value;
    }

    /**
     * 尝试把当前字符串按 GBK 字节重新解释为 UTF-8。
     * 如果转换失败，则保留原始值。
     */
    private String repairGbkDecodedUtf8(String value) {
        try {
            return new String(value.getBytes(gbk), StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    /**
     * 给解码后的文本计算乱码分数。
     * 替换字符和已知乱码片段都会提高分数。
     */
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

    /**
     * 面向当前样例和上传文件的轻量级 CSV 分割器。
     * 它会移除 UTF-8 BOM，并去掉逗号两侧的空格。
     */
    private String[] splitCsvLine(String line) {
        return line.replace("\uFEFF", "").split("\\s*,\\s*");
    }
}
