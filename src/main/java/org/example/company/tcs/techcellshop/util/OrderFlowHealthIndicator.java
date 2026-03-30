package org.example.company.tcs.techcellshop.util;

import org.example.company.tcs.techcellshop.repository.ProcessedEventRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OrderFlowHealthIndicator implements HealthIndicator {

    private final ProcessedEventRepository processedEventRepository;

    public OrderFlowHealthIndicator(ProcessedEventRepository processedEventRepository){
        this.processedEventRepository = processedEventRepository;
    }

    @Override
    public Health health() {
        try{
            long count = processedEventRepository.count();
            return Health.up().withDetail("processedEvents", count).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("Component", "orderFlow").build();
        }
    }
}
