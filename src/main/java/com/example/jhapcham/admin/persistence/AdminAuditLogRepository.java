package com.example.jhapcham.admin.persistence;

import com.example.jhapcham.admin.domain.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findByActionContainingIgnoreCaseOrTargetTypeContainingIgnoreCaseOrActorUsernameContainingIgnoreCase(
            String action,
            String targetType,
            String actorUsername,
            Pageable pageable
    );
}
