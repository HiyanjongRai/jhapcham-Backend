package com.example.jhapcham.product.api;

import com.example.jhapcham.product.application.*;
import com.example.jhapcham.product.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomepageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Test
    void testGetBestSellers_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/best-sellers")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopBestSellers_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/best-sellers/top")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopRated_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/top-rated")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopRatedProducts_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/top-rated/top")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetMostWishlisted_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/most-wishlisted")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopMostWishlisted_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/most-wishlisted/top")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTrending_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/trending")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopTrending_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/trending/top")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRecommendations_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/recommendations")
                .param("limit", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetRecommendationsByCategory_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/products/recommendations")
                .param("limit", "20")
                .param("category", "Electronics"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetBestSellers_WithInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/products/best-sellers")
                .param("page", "0")
                .param("size", "1000"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetBestSellers_WithNegativePage() throws Exception {
        mockMvc.perform(get("/api/products/best-sellers")
                .param("page", "-1")
                .param("size", "20"))
                .andExpect(status().isOk());
    }
}
