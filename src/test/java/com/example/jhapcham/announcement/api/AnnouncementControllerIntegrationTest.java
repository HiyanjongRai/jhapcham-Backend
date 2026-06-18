package com.example.jhapcham.announcement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.jhapcham.announcement.dto.AnnouncementDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnouncementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AnnouncementDTO testAnnouncement;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        testAnnouncement = AnnouncementDTO.builder()
                .title("Test Announcement")
                .description("Test Description")
                .content("Test Content")
                .type("PROMOTION")
                .priority("HIGH")
                .status("ACTIVE")
                .imageUrl("test.jpg")
                .actionUrl("/test")
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(7))
                .isActive(true)
                .displayOrder(1)
                .build();
    }

    @Test
    void testGetAnnouncements_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/announcements")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAnnouncementsList_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/announcements/list"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetLatestAnnouncements_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/announcements/latest")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAnnouncementsByPriority_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/announcements/priority/HIGH"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAnnouncementsByType_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/announcements/type/PROMOTION"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateAnnouncement_WithAdminRole_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncement)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateAnnouncement_WithUserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncement)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateAnnouncement_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncement)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateAnnouncement_WithAdminRole_ReturnsOk() throws Exception {
        mockMvc.perform(put("/api/announcements/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncement)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateAnnouncement_WithUserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/announcements/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAnnouncement)))
                .andExpect(status().isForbidden());
    }
}
