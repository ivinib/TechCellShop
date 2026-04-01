package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.example.company.tcs.techcellshop.util.DiscountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tb_coupon")
@Data
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_coupon")
    private Long id;

    @Column(name = "code_coupon", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_coupon", nullable = false, length = 20)
    private DiscountType type;

    @Column(name = "value_coupon", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "active_coupon", nullable = false)
    private Boolean active;

    @Column(name = "starts_at_coupon")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at_coupon")
    private OffsetDateTime endsAt;

    @Column(name = "min_order_amount_coupon", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_uses_coupon")
    private Integer maxUses;

    @Column(name = "used_count_coupon")
    private Integer usedCount;
}
