package com.example.jhapcham;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Automatically drops restrictive PostgreSQL check constraints that block 
 * new order statuses like DRAFT and REQUIRES_PAYMENT.
 */
@Slf4j
@Component
@Profile("schema-fix")
@RequiredArgsConstructor
public class DatabaseFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Aggressively cleaning up restrictive database constraints on 'orders' table...");
        
        try {
            // Find all check constraints on the 'orders' table
            String sql = "SELECT constraint_name FROM information_schema.constraint_column_usage " +
                         "WHERE table_name = 'orders' AND constraint_name LIKE '%_check%'";
            
            java.util.List<String> constraints = jdbcTemplate.queryForList(sql, String.class);
            
            if (constraints.isEmpty()) {
                log.info("No check constraints found on 'orders' table.");
            } else {
                for (String constraintName : constraints) {
                    try {
                        log.info("Dropping constraint: {}", constraintName);
                        jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS " + constraintName);
                    } catch (Exception e) {
                        log.warn("Failed to drop constraint {}: {}", constraintName, e.getMessage());
                    }
                }
                log.info("Finished dropping {} constraints.", constraints.size());
            }
            
            log.info("Database schema fix completed successfully.");
        } catch (Exception e) {
            log.error("DatabaseFixer failed to query constraints: {}", e.getMessage());
        }
    }
}
