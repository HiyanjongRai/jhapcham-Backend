package com.example.jhapcham.order.application;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-readable, sequential order IDs for Jhapcham.
 *
 * <p>Format: {@code JHC-YYYYMMDD-XXXX}
 * <ul>
 *   <li>{@code JHC}      – fixed brand prefix</li>
 *   <li>{@code YYYYMMDD} – current date (Nepal local date passed in by the caller)</li>
 *   <li>{@code XXXX}     – 4-digit zero-padded daily sequence, resets at midnight</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>
 *   JHC-20260520-0001
 *   JHC-20260520-0002
 *   JHC-20260521-0001  ← resets next day
 * </pre>
 *
 * <p><strong>Concurrency:</strong> The method is {@code synchronized} so only one thread
 * can mint a new ID at a time within a single JVM instance.  For multi-node deployments,
 * the {@code UNIQUE} constraint on {@code custom_order_id} acts as the ultimate safety net
 * – a constraint violation will propagate as a {@link org.springframework.dao.DataIntegrityViolationException}
 * and the caller should retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdGenerator {

    private static final String PREFIX        = "JHC";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int    MAX_DAILY_SEQ = 9999;

    private final OrderRepository orderRepository;

    /**
     * Generates the next available order ID for today.
     *
     * @param today the current date (pass {@link LocalDate#now()} from the call site)
     * @return a unique order ID string, e.g. {@code JHC-20260520-0003}
     * @throws IllegalStateException if the daily sequence limit of 9999 is exceeded
     */
    public synchronized String generate(LocalDate today) {
        String datePart  = today.format(DATE_FMT);
        String dayPrefix = PREFIX + "-" + datePart + "-";

        // Find the latest order created today (any custom_order_id starting with today's prefix).
        int nextSequence = orderRepository
                .findMaxDailySequence(dayPrefix)
                .map(max -> max + 1)
                .orElse(1);

        if (nextSequence > MAX_DAILY_SEQ) {
            throw new IllegalStateException(
                    "Daily order sequence limit (" + MAX_DAILY_SEQ + ") exceeded for date " + datePart);
        }

        String customOrderId = String.format("%s-%s-%04d", PREFIX, datePart, nextSequence);
        log.debug("Generated customOrderId: {}", customOrderId);
        return customOrderId;
    }
}
