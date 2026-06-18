package com.example.jhapcham.admin.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminStatisticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetDashboardStatistics_WithAdminRole_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetDashboardStatistics_WithUserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetDashboardStatistics_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRefreshDashboardStatistics_WithAdminRole_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/admin/statistics/dashboard/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testRefreshDashboardStatistics_WithUserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/statistics/dashboard/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRefreshDashboardStatistics_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/statistics/dashboard/refresh"))
                .andExpect(status().isUnauthorized());
    }
}
