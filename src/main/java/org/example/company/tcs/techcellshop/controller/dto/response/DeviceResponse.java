package org.example.company.tcs.techcellshop.controller.dto.response;

public record DeviceResponse (
        Long idDevice,
        String nameDevice,
        String descriptionDevice,
        String deviceType,
        String deviceStorage,
        String deviceRam,
        String deviceColor,
        Double devicePrice,
        Integer deviceStock,
        String deviceCondition
){}
