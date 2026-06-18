package com.example.jhapcham.admin.application;

import com.example.jhapcham.admin.domain.AdminAuditLog;
import com.example.jhapcham.admin.dto.AdminAuditLogDTO;
import com.example.jhapcham.admin.persistence.AdminAuditLogRepository;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, Long targetId, String summary) {
        record(action, targetType, targetId, summary, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, Long targetId, String summary, String metadata) {
        try {
            User actor = resolveActor();
            adminAuditLogRepository.save(AdminAuditLog.builder()
                    .actor(actor)
                    .actorUsername(actor != null ? actor.getUsername() : "SYSTEM")
                    .action(safe(action, 80))
                    .targetType(safe(targetType, 60))
                    .targetId(targetId)
                    .summary(safe(summary, 500))
                    .metadata(metadata)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to write admin audit log for action {} on {}#{}", action, targetType, targetId, ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogDTO> search(String query, Pageable pageable) {
        Page<AdminAuditLog> page = (query == null || query.isBlank())
                ? adminAuditLogRepository.findAll(pageable)
                : adminAuditLogRepository.findByActionContainingIgnoreCaseOrTargetTypeContainingIgnoreCaseOrActorUsernameContainingIgnoreCase(
                        query.trim(), query.trim(), query.trim(), pageable);
        return page.map(this::toDto);
    }

    private User resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        String principal = authentication.getName();
        return userRepository.findByUsernameOrEmail(principal, principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElse(null);
    }

    private AdminAuditLogDTO toDto(AdminAuditLog log) {
        return AdminAuditLogDTO.builder()
                .id(log.getId())
                .actorId(log.getActor() != null ? log.getActor().getId() : null)
                .actorUsername(log.getActorUsername())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .summary(log.getSummary())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String safe(String value, int maxLength) {
        String normalized = value == null || value.isBlank() ? "N/A" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
