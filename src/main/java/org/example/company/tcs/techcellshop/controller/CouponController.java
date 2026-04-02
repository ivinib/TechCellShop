package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationRequestDto;
import org.example.company.tcs.techcellshop.controller.dto.coupon.CouponValidationResponseDto;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.example.company.tcs.techcellshop.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Coupon Management", description = "Endpoints for coupon validation and discount checks")
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(
            summary = "Validate coupon",
            description = "Validates coupon code against order amount and returns discount details"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon validation executed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/ValidationErrorExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Coupon business rule conflict",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/BusinessConflictExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InvalidArgumentExample")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(ref = "#/components/examples/InternalErrorExample")
                    )
            )
    })
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponseDto> validate(
            @Valid @RequestBody CouponValidationRequestDto request) {
        return ResponseEntity.ok(couponService.validateCoupon(request.getCode(), request.getOrderAmount()));
    }
}
