package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tb_order_idempondency", uniqueConstraints = @UniqueConstraint(name = "uk_order_idempondency_key", columnNames = "idempondency_key"))
@Data
public class OrderIdempondency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order_idempondency")
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order", nullable = false)
    private Order order;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
