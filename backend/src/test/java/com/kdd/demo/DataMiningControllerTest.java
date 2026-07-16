package com.kdd.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "kdd.use-mysql=false",
        "kdd.python-command=python"
})
@AutoConfigureMockMvc
class DataMiningControllerTest {
    /**
     * MockMvc 可以在不真正启动浏览器的情况下调用控制器接口，
     * 用来验证后端 JSON 响应结构是否符合前端预期。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证健康检查、Iris 数据和事务数据接口都能返回基础演示数据。
     */
    @Test
    void healthAndDataEndpointsReturnDemoData() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend").value("spring-boot"));

        mockMvc.perform(get("/api/iris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0)));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0)));
    }

    /**
     * 验证四类数据挖掘接口可以完成调用，并返回关键结果字段。
     */
    @Test
    void analysisEndpointsCallPythonAndReturnResults() throws Exception {
        mockMvc.perform(post("/api/clustering")
                        .contentType("application/json")
                        .content("{\"k\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()", greaterThan(0)));

        mockMvc.perform(post("/api/classification")
                        .contentType("application/json")
                        .content("{\"max_depth\":3,\"min_leaf\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accuracy").exists());

        mockMvc.perform(post("/api/regression")
                        .contentType("application/json")
                        .content("{\"x_field\":\"petal_length\",\"y_field\":\"petal_width\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.r2").exists());

        mockMvc.perform(post("/api/association")
                        .contentType("application/json")
                        .content("{\"min_support\":0.2,\"min_confidence\":0.4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules").isArray());
    }

    /**
     * 验证上传 GBK 编码中文 CSV 时，后端能够正确识别并修复中文内容。
     */
    @Test
    void uploadedTransactionCsvSupportsChineseGbk() throws Exception {
        byte[] content = "transaction_id,items\n1,牛奶,面包\n2,牛奶,鸡蛋\n"
                .getBytes(Charset.forName("GBK"));
        MockMultipartFile file = new MockMultipartFile("file", "transactions.csv", "text/csv", content);

        mockMvc.perform(multipart("/api/transactions/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0][0]").value("牛奶"))
                .andExpect(jsonPath("$.transactions[0][1]").value("面包"));
    }
}
