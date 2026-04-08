package org.example.company.tcs.techcellshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.outbox")
@Validated
public record OutboxProperties(
        long pollIntervalMs,
        int failedThreshold
) {}