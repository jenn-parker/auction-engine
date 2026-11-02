package com.jennparker.auction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies Flyway can build the authoritative schema from an empty PostgreSQL database. */
@Testcontainers
@SpringBootTest
class MigrationIntegrationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auction_engine")
            .withUsername("auction")
            .withPassword("auction");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCreatesAllRequiredTablesAndHibernateValidationSucceeds() {
        List<String> requiredTables = Arrays.asList(
                "bids", "orders", "payment_attempts", "outbox_events",
                "payments", "payment_allocations", "customer_credit_ledger",
                "customers", "payment_methods", "auctions", "lots");

        for (String table : requiredTables) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, table);
            assertTrue(count != null && count == 1, "Missing migrated table: " + table);
        }

        Integer migrations = jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
        assertTrue(migrations != null && migrations >= 16, "Expected all versioned migrations to run");
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("AWS_ENDPOINT", () -> "");
        registry.add("PAYMENT_OUTBOX_ENABLED", () -> "false");
        registry.add("PAYMENT_WORKER_ENABLED", () -> "false");
        registry.add("SECURITY_COGNITO_ENABLED", () -> "false");
    }
}
