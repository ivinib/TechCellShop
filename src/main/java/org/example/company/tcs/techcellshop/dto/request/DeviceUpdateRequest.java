package org.example.company.tcs.techcellshop.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceUpdateRequest {

    @Schema(example = "Galaxy S26")
    @NotBlank(message = "Device name is required")
    @Size(min = 2, max = 120, message = "Device name must have between 2 and 120 characters")
    private String nameDevice;

    @Schema(example = "Latest smartphone with the most advanced features currently in the market")
    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 255, message = "Description must have between 5 and 255 characters")
    private String descriptionDevice;

    @Schema(name = "SMARTPHONE")
    @NotBlank(message = "Device type is required")
    @Pattern(
            regexp = "^(COMPUTER|LAPTOP|TABLET|SMARTPHONE)$",
            message = "Device type must be COMPUTER, LAPTOP, TABLET or SMARTPHONE"
    )
    private String deviceType;

    @Schema(example = "256gb")
    @NotBlank(message = "Storage is required")
    @Pattern(regexp = "^\\d+(GB|TB)$", message = "Storage must be like 128GB, 512GB or 1TB")
    private String deviceStorage;

    @Schema(example = "12gb")
    @NotBlank(message = "RAM is required")
    @Pattern(regexp = "^\\d+GB$", message = "RAM must be like 8GB or 16GB")
    private String deviceRam;

    @Schema(example = "Black")
    @NotBlank(message = "Color is required")
    @Size(min = 2, max = 50, message = "Color must have between 2 and 50 characters")
    private String deviceColor;

    @Schema(example = "3999.99")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Price must have up to 10 integer digits and 2 decimal places")
    private BigDecimal devicePrice;

    @Schema(example = "20")
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer deviceStock;

    @Schema(example = "NEW")
    @NotBlank(message = "Condition is required")
    @Pattern(
            regexp = "^(NEW|USED|REFURBISHED)$",
            message = "Condition must be NEW, USED or REFURBISHED"
    )
    private String deviceCondition;
}
