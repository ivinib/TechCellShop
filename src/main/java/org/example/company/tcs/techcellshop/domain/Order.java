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

    @Column(name = "quantity_order")
    private Integer quantityOrder;

    @Column(name = "total_price_order")
    private Double totalPriceOrder;

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
