package com.kdd.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "kdd.use-mysql=false",
        "kdd.python-command=python"
})
@AutoConfigureMockMvc
class DataMiningControllerTest {
    @Autowired
    private MockMvc mockMvc;

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
}
