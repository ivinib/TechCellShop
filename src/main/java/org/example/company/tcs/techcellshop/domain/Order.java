package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    @Column(name = "status_order")
    private String statusOrder;

    @Column(name = "order_date")
    private String orderDate;

    @Column(name = "delivery_date")
    private String deliveryDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus;
}
