package com.example.jhapcham.announcement.application;

import com.example.jhapcham.announcement.domain.Announcement;
import com.example.jhapcham.announcement.dto.AnnouncementDTO;
import com.example.jhapcham.announcement.persistence.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private Announcement mockAnnouncement;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        mockAnnouncement = Announcement.builder()
                .id(1L)
                .title("New Sale")
                .description("Summer sale announcement")
                .content("Get 50% off on all products")
                .type("PROMOTION")
                .priority("HIGH")
                .status("ACTIVE")
                .imageUrl("sale.jpg")
                .actionUrl("/sale")
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(7))
                .isActive(true)
                .displayOrder(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void testGetActiveAnnouncements_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Announcement> announcements = new ArrayList<>();
        announcements.add(mockAnnouncement);
        Page<Announcement> mockPage = new PageImpl<>(announcements, pageable, 1);
        
        when(announcementRepository.findActiveAnnouncements(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<AnnouncementDTO> result = announcementService.getActiveAnnouncements(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("New Sale", result.getContent().get(0).getTitle());
    }

    @Test
    void testGetActiveAnnouncementsList_Success() {
        List<Announcement> announcements = new ArrayList<>();
        announcements.add(mockAnnouncement);
        
        when(announcementRepository.findActiveAnnouncementsList(any(LocalDateTime.class)))
                .thenReturn(announcements);

        List<AnnouncementDTO> result = announcementService.getActiveAnnouncementsList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("New Sale", result.get(0).getTitle());
    }

    @Test
    void testGetLatestAnnouncements_Success() {
        List<Announcement> announcements = new ArrayList<>();
        announcements.add(mockAnnouncement);
        
        when(announcementRepository.findLatestAnnouncements(10))
                .thenReturn(announcements);

        List<AnnouncementDTO> result = announcementService.getLatestAnnouncements(10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAnnouncementsByPriority_Success() {
        List<Announcement> announcements = new ArrayList<>();
        announcements.add(mockAnnouncement);
        
        when(announcementRepository.findByPriority("HIGH"))
                .thenReturn(announcements);

        List<AnnouncementDTO> result = announcementService.getAnnouncementsByPriority("HIGH");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("HIGH", result.get(0).getPriority());
    }

    @Test
    void testGetAnnouncementsByType_Success() {
        List<Announcement> announcements = new ArrayList<>();
        announcements.add(mockAnnouncement);
        
        when(announcementRepository.findByType("PROMOTION"))
                .thenReturn(announcements);

        List<AnnouncementDTO> result = announcementService.getAnnouncementsByType("PROMOTION");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PROMOTION", result.get(0).getType());
    }

    @Test
    void testCreateAnnouncement_Success() {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title("New Sale")
                .description("Summer sale announcement")
                .content("Get 50% off on all products")
                .type("PROMOTION")
                .priority("HIGH")
                .status("ACTIVE")
                .imageUrl("sale.jpg")
                .actionUrl("/sale")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .displayOrder(1)
                .build();
        
        when(announcementRepository.save(any(Announcement.class)))
                .thenReturn(mockAnnouncement);

        AnnouncementDTO result = announcementService.createAnnouncement(dto);

        assertNotNull(result);
        assertEquals("New Sale", result.getTitle());
    }

    @Test
    void testUpdateAnnouncement_Success() {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title("Updated Sale")
                .description("Updated description")
                .content("Updated content")
                .type("PROMOTION")
                .priority("MEDIUM")
                .status("ACTIVE")
                .imageUrl("sale.jpg")
                .actionUrl("/sale")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .displayOrder(1)
                .build();
        
        when(announcementRepository.findById(1L))
                .thenReturn(Optional.of(mockAnnouncement));
        when(announcementRepository.save(any(Announcement.class)))
                .thenReturn(mockAnnouncement);

        AnnouncementDTO result = announcementService.updateAnnouncement(1L, dto);

        assertNotNull(result);
    }
}
