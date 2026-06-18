package com.example.jhapcham.announcement.persistence;

import com.example.jhapcham.announcement.domain.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND a.startDate <= :now AND a.endDate >= :now ORDER BY a.displayOrder DESC, a.createdAt DESC")
    Page<Announcement> findActiveAnnouncements(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.isActive = true AND a.startDate <= :now AND a.endDate >= :now ORDER BY a.displayOrder DESC, a.createdAt DESC")
    List<Announcement> findActiveAnnouncementsList(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a WHERE a.status = :status ORDER BY a.displayOrder DESC, a.createdAt DESC")
    Page<Announcement> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.priority = :priority AND a.isActive = true ORDER BY a.displayOrder DESC")
    List<Announcement> findByPriority(@Param("priority") String priority);

    @Query("SELECT a FROM Announcement a WHERE a.type = :type AND a.isActive = true ORDER BY a.displayOrder DESC")
    List<Announcement> findByType(@Param("type") String type);

    @Query("SELECT a FROM Announcement a WHERE a.isActive = true ORDER BY a.displayOrder DESC, a.createdAt DESC LIMIT :limit")
    List<Announcement> findLatestAnnouncements(@Param("limit") int limit);
}
