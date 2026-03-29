package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity(name = "tb_processed_event")
@Data
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_processed_event")
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
