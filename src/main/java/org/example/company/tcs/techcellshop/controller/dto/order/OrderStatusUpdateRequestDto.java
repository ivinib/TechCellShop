package org.example.company.tcs.techcellshop.controller.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.company.tcs.techcellshop.util.OrderStatus;

@Data
public class OrderStatusUpdateRequestDto {

    @NotNull
    private OrderStatus newStatus;

    private String reason;
}
