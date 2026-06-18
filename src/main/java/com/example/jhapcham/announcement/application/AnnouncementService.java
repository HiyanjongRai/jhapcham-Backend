package com.example.jhapcham.announcement.application;

import com.example.jhapcham.announcement.domain.Announcement;
import com.example.jhapcham.announcement.dto.AnnouncementDTO;
import com.example.jhapcham.announcement.persistence.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Cacheable(cacheNames = "announcements", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<AnnouncementDTO> getActiveAnnouncements(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Announcement> results = announcementRepository.findActiveAnnouncements(now, pageable);
        List<AnnouncementDTO> dtos = results.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    @Cacheable(cacheNames = "announcements-list", key = "'all'")
    public List<AnnouncementDTO> getActiveAnnouncementsList() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> results = announcementRepository.findActiveAnnouncementsList(now);
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "announcements-latest", key = "#limit")
    public List<AnnouncementDTO> getLatestAnnouncements(int limit) {
        List<Announcement> results = announcementRepository.findLatestAnnouncements(limit);
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "announcements-priority", key = "#priority")
    public List<AnnouncementDTO> getAnnouncementsByPriority(String priority) {
        List<Announcement> results = announcementRepository.findByPriority(priority);
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "announcements-type", key = "#type")
    public List<AnnouncementDTO> getAnnouncementsByType(String type) {
        List<Announcement> results = announcementRepository.findByType(type);
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(cacheNames = {"announcements", "announcements-list", "announcements-latest", "announcements-priority", "announcements-type"}, allEntries = true)
    public AnnouncementDTO createAnnouncement(AnnouncementDTO dto) {
        Announcement announcement = Announcement.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .content(dto.getContent())
                .type(dto.getType())
                .priority(dto.getPriority())
                .status(dto.getStatus())
                .imageUrl(dto.getImageUrl())
                .actionUrl(dto.getActionUrl())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .build();
        
        Announcement saved = announcementRepository.save(announcement);
        return mapToDTO(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"announcements", "announcements-list", "announcements-latest", "announcements-priority", "announcements-type"}, allEntries = true)
    public AnnouncementDTO updateAnnouncement(Long id, AnnouncementDTO dto) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
        
        announcement.setTitle(dto.getTitle());
        announcement.setDescription(dto.getDescription());
        announcement.setContent(dto.getContent());
        announcement.setType(dto.getType());
        announcement.setPriority(dto.getPriority());
        announcement.setStatus(dto.getStatus());
        announcement.setImageUrl(dto.getImageUrl());
        announcement.setActionUrl(dto.getActionUrl());
        announcement.setStartDate(dto.getStartDate());
        announcement.setEndDate(dto.getEndDate());
        announcement.setIsActive(dto.getIsActive());
        announcement.setDisplayOrder(dto.getDisplayOrder());
        
        Announcement updated = announcementRepository.save(announcement);
        return mapToDTO(updated);
    }

    private AnnouncementDTO mapToDTO(Announcement announcement) {
        return AnnouncementDTO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .description(announcement.getDescription())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .status(announcement.getStatus())
                .imageUrl(announcement.getImageUrl())
                .actionUrl(announcement.getActionUrl())
                .startDate(announcement.getStartDate())
                .endDate(announcement.getEndDate())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .isActive(announcement.getIsActive())
                .displayOrder(announcement.getDisplayOrder())
                .build();
    }
}
