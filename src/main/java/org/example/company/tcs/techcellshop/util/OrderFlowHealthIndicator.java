package org.example.company.tcs.techcellshop.util;

import org.example.company.tcs.techcellshop.config.OutboxProperties;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.repository.ProcessedEventRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class OrderFlowHealthIndicator implements HealthIndicator {

    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final int failedThreshold;

    public OrderFlowHealthIndicator(
            ProcessedEventRepository processedEventRepository,
            OutboxEventRepository outboxEventRepository,
            OutboxProperties outboxProperties
    ) {
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.failedThreshold = outboxProperties.failedThreshold();
    }
    @Override
    public Health health() {
        try {
            long processedCount = processedEventRepository.count();
            long pendingCount = outboxEventRepository.countByStatus(OutboxEventStatus.PENDING);
            long failedCount = outboxEventRepository.countByStatus(OutboxEventStatus.FAILED);

            Long oldestPendingAgeSeconds = outboxEventRepository
                    .findOldestCreatedAtByStatus(OutboxEventStatus.PENDING)
                    .map(ts -> Duration.between(ts, Instant.now()).toSeconds())
                    .orElse(0L);

            Health.Builder builder = failedCount > failedThreshold ? Health.down() : Health.up();

            return builder
                    .withDetail("processedEvents", processedCount)
                    .withDetail("outboxPending", pendingCount)
                    .withDetail("outboxFailed", failedCount)
                    .withDetail("failedThreshold", failedThreshold)
                    .withDetail("oldestPendingAgeSeconds", oldestPendingAgeSeconds)
                    .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("component", "orderFlow").build();
        }
    }
}
