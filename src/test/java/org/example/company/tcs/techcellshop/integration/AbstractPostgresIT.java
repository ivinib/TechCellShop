package org.example.company.tcs.techcellshop.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class AbstractPostgresIT {
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("techcellshop_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Disable RabbitMQ interaction for tests that only need Postgres
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> false);
        registry.add("spring.rabbitmq.dynamic", () -> false);
        registry.add("management.health.rabbit.enabled", () -> false);

        // Avoid readiness depending on rabbit for Postgres-only tests
        registry.add("management.endpoint.health.group.readiness.include",
                () -> "readinessState,db,orderFlow");

        // Keep the scheduled outbox publisher asleep during these tests
        registry.add("app.outbox.poll-interval-ms", () -> "86400000");
    }
}
