package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.example.company.tcs.techcellshop.dto.OutboxEventResponseDto;
import org.example.company.tcs.techcellshop.dto.OutboxRequeueRequestDto;
import org.example.company.tcs.techcellshop.dto.RequeueResultDto;
import org.example.company.tcs.techcellshop.service.OutboxAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Validated
@RestController
@RequestMapping("/api/v1/admin/outbox")
@Tag(name = "Outbox Admin", description = "Operational endpoints for failed outbox event recovery")
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    public OutboxAdminController(OutboxAdminService outboxAdminService) {
        this.outboxAdminService = outboxAdminService;
    }

    @Operation(
            summary = "List failed outbox events",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @GetMapping("/failed")
    public ResponseEntity<Page<OutboxEventResponseDto>> listFailed(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(outboxAdminService.listFailed(page, size));
    }

    @Operation(
            summary = "Requeue a single failed outbox event",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PostMapping("/{id}/requeue")
    public ResponseEntity<RequeueResultDto> requeueOne(@PathVariable Long id) {
        return ResponseEntity.ok(outboxAdminService.requeueOne(id));
    }

    @Operation(
            summary = "Requeue multiple failed outbox events",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PostMapping("/requeue")
    public ResponseEntity<RequeueResultDto> requeueBatch(@Valid @RequestBody OutboxRequeueRequestDto request) {
        return ResponseEntity.ok(outboxAdminService.requeueFailed(request.ids()));
    }
}