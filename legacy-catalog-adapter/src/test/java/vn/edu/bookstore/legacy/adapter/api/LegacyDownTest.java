package vn.edu.bookstore.legacy.adapter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "bookstore.legacy.endpoint=http://localhost:1/ws/catalog",
        "bookstore.legacy.connect-timeout=500ms",
        "bookstore.legacy.read-timeout=1000ms"
})
@AutoConfigureMockMvc
class LegacyDownTest {

    @Autowired
    MockMvc mvc;

    @Test
    void heThongKeThuaKhongHoatDongTraVe503() throws Exception {
        mvc.perform(get("/api/v1/legacy/books/9780134685991"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type")
                        .value("https://api.bookstore.vn/problems/upstream-unavailable"))
                .andExpect(jsonPath("$.legacyCode")
                        .value("UPSTREAM_UNAVAILABLE"));
    }
}

