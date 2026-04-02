package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "tb_device")
@Data
@EqualsAndHashCode
@ToString
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_device")
    private Long idDevice;

    @Column(name = "name_device")
    private String nameDevice;

    @Column(name = "description_device")
    private String descriptionDevice;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "device_storage")
    private String deviceStorage;

    @Column(name = "device_ram")
    private String deviceRam;

    @Column(name = "device_color")
    private String deviceColor;

    @Column(name = "device_price")
    private Double devicePrice;

    @Column(name = "device_stock")
    private Integer deviceStock;

    @Column(name = "device_condition")
    private String deviceCondition;

    @Version
    @Column(name = "version")
    private Long version;

}
