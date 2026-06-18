package com.example.jhapcham.analytics.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Test
    void testGetPopularSearches_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/analytics/popular-searches")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopPopularSearches_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/analytics/popular-searches/top")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRecentSearches_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/analytics/popular-searches/recent")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopConversionSearches_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/analytics/popular-searches/conversion")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetPopularSearches_WithPagination() throws Exception {
        mockMvc.perform(get("/api/analytics/popular-searches")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetPopularSearches_WithLargePageSize() throws Exception {
        mockMvc.perform(get("/api/analytics/popular-searches")
                .param("page", "0")
                .param("size", "1000"))
                .andExpect(status().isOk());
    }
}
