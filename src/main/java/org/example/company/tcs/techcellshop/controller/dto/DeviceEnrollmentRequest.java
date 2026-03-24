package org.example.company.tcs.techcellshop.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DeviceEnrollmentRequest {
    @NotBlank(message = "Device name is required")
    @Size(min = 2, max = 120, message = "Device name must have between 2 and 120 characters")
    private String nameDevice;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 255, message = "Description must have between 5 and 255 characters")
    private String descriptionDevice;

    @NotBlank(message = "Device type is required")
    @Pattern(
            regexp = "^(COMPUTER|LAPTOP|TABLET|SMARTPHONE)$",
            message = "Device type must be COMPUTER, LAPTOP, TABLET or SMARTPHONE"
    )
    private String deviceType;

    @NotBlank(message = "Storage is required")
    @Pattern(regexp = "^\\d+(GB|TB)$", message = "Storage must be like 128GB, 512GB or 1TB")
    private String deviceStorage;

    @NotBlank(message = "RAM is required")
    @Pattern(regexp = "^\\d+GB$", message = "RAM must be like 8GB or 16GB")
    private String deviceRam;

    @NotBlank(message = "Color is required")
    @Size(min = 2, max = 50, message = "Color must have between 2 and 50 characters")
    private String deviceColor;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private Double devicePrice;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer deviceStock;

    @NotBlank(message = "Condition is required")
    @Pattern(
            regexp = "^(NEW|USED|REFURBISHED)$",
            message = "Condition must be NEW, USED or REFURBISHED"
    )
    private String deviceCondition;
}
