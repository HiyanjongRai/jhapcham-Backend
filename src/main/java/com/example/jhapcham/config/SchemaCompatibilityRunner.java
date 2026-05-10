package com.example.jhapcham.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("schema-fix")
@RequiredArgsConstructor
@Slf4j
public class SchemaCompatibilityRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        migrateOrderStatuses();
        migrateDeliveryStatuses();
        migratePaymentStatuses();
    }

    private void migrateOrderStatuses() {
        executeSafely("UPDATE orders SET status = 'PENDING' WHERE status = 'NEW'");
        executeSafely("UPDATE orders SET status = 'SHIPPED' WHERE status = 'SHIPPED_TO_BRANCH'");
        executeSafely("UPDATE orders SET status = 'CANCELLED' WHERE status = 'CANCELED'");
        executeSafely("UPDATE orders SET status = 'REFUNDED' WHERE status = 'PARTIALLY_REFUNDED'");

        executeSafely("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check");
        executeSafely("""
                ALTER TABLE orders
                ADD CONSTRAINT orders_status_check
                CHECK (status IN (
                    'DRAFT',
                    'PENDING',
                    'COD_PENDING',
                    'CONFIRMED',
                    'CONFIRMED_BY_CALL',
                    'PROCESSING',
                    'PACKED',
                    'SHIPPED',
                    'OUT_FOR_DELIVERY',
                    'DELIVERED',
                    'CANCELLED',
                    'RETURN_REQUESTED',
                    'RETURNED',
                    'REFUNDED',
                    'FAILED'
                ))
                """);
    }

    private void migrateDeliveryStatuses() {
        executeSafely("UPDATE shipments SET status = 'CREATED' WHERE status IN ('ORDER_PLACED', 'PACKED')");
        executeSafely("UPDATE deliveries SET status = 'CREATED' WHERE status IN ('ORDER_PLACED', 'PACKED')");
        executeSafely("UPDATE tracking_history SET status = 'CREATED' WHERE status IN ('ORDER_PLACED', 'PACKED')");

        executeSafely("ALTER TABLE shipments DROP CONSTRAINT IF EXISTS shipments_status_check");
        executeSafely("ALTER TABLE deliveries DROP CONSTRAINT IF EXISTS deliveries_status_check");
        executeSafely("ALTER TABLE tracking_history DROP CONSTRAINT IF EXISTS tracking_history_status_check");

        String deliveryCheck = """
                CHECK (status IN (
                    'CREATED',
                    'RIDER_ASSIGNED',
                    'PICKED_UP',
                    'IN_TRANSIT',
                    'OUT_FOR_DELIVERY',
                    'DELIVERED',
                    'FAILED_DELIVERY',
                    'RETURN_TO_SELLER',
                    'DELAYED',
                    'CANCELLED',
                    'CALL_NOT_PICKED',
                    'ADDRESS_NOT_FOUND'
                ))
                """;

        executeSafely("ALTER TABLE shipments ADD CONSTRAINT shipments_status_check " + deliveryCheck);
        executeSafely("ALTER TABLE deliveries ADD CONSTRAINT deliveries_status_check " + deliveryCheck);
        executeSafely("ALTER TABLE tracking_history ADD CONSTRAINT tracking_history_status_check " + deliveryCheck);
    }

    private void migratePaymentStatuses() {
        executeSafely("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status varchar(255)");
        executeSafely("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_initiated_at timestamp");
        executeSafely("ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at timestamp");

        executeSafely("""
                UPDATE orders
                SET payment_status = CASE
                    WHEN payment_reference IS NOT NULL AND payment_reference <> '' THEN 'PAID'
                    WHEN payment_method = 'COD' THEN 'PENDING_COD'
                    WHEN payment_method = 'ESEWA' THEN 'REQUIRES_PAYMENT'
                    WHEN payment_method = 'STRIPE' THEN 'REQUIRES_PAYMENT'
                    ELSE 'PENDING'
                END
                WHERE payment_status IS NULL
                """);

        executeSafely("ALTER TABLE orders ALTER COLUMN payment_status SET DEFAULT 'PENDING'");
        executeSafely("ALTER TABLE orders ALTER COLUMN payment_status SET NOT NULL");
        executeSafely("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check");
        executeSafely("""
                ALTER TABLE orders
                ADD CONSTRAINT orders_payment_status_check
                CHECK (payment_status IN (
                    'PENDING',
                    'REQUIRES_PAYMENT',
                    'PAYMENT_INITIATED',
                    'PAID',
                    'PENDING_VERIFICATION',
                    'PENDING_COD',
                    'COD_COLLECTED',
                    'COD_FAILED',
                    'COD_REMITTED',
                    'REFUND_PENDING',
                    'REFUNDED',
                    'FAILED',
                    'CANCELLED',
                    'EXPIRED'
                ))
                """);
    }

    private void executeSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception exception) {
            log.warn("Schema compatibility statement skipped: {}", exception.getMessage());
        }
    }
}
