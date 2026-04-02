package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;

import java.time.Instant;

@Entity
@Table(name = "tb_outbox_event")
@Data
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_outbox_event")
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_outbox", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "attempts_outbox", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at_outbox", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at_outbox")
    private Instant sentAt;

    @Column(name = "last_error_outbox", length = 512)
    private String lastError;
}
