package org.example.company.tcs.techcellshop.dto.response;

import java.math.BigDecimal;

public record DeviceResponse (
        Long idDevice,
        String nameDevice,
        String descriptionDevice,
        String deviceType,
        String deviceStorage,
        String deviceRam,
        String deviceColor,
        BigDecimal devicePrice,
        Integer deviceStock,
        String deviceCondition
){}
