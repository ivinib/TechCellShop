package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;

import java.math.BigDecimal;

@Entity(name = "tb_order")
@Data
@EqualsAndHashCode
@ToString
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_order")
    private Long idOrder;

    @ManyToOne
    @JoinColumn(name = "id_user")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_device")
    private Device device;

    @Column(name = "user_id_snapshot")
    private Long userIdSnapshot;

    @Column(name = "user_name_snapshot", length = 120)
    private String userNameSnapshot;

    @Column(name = "user_email_snapshot", length = 150)
    private String userEmailSnapshot;

    @Column(name = "device_id_snapshot")
    private Long deviceIdSnapshot;

    @Column(name = "device_name_snapshot", length = 120)
    private String deviceNameSnapshot;

    @Column(name = "unit_price_snapshot", precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity_order")
    private Integer quantityOrder;

    @Column(name = "total_price_order", precision = 12, scale = 2)
    private BigDecimal totalPriceOrder;

    @Version
    @Column(name = "version")
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_order", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "order_date")
    private String orderDate;

    @Column(name = "delivery_date")
    private String deliveryDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status_order", length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "canceled_reason_order", length = 255)
    private String canceledReason;

    @Column(name = "coupon_code_order", length = 50)
    private String couponCode;

    @Column(name = "discount_amount_order", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount_order", precision = 12, scale = 2)
    private BigDecimal finalAmount;
}
