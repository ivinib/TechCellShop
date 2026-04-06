package org.example.company.tcs.techcellshop.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OutboxRequeueRequestDto(
        @NotEmpty(message = "At least one outbox event id must be informed")
        List<Long> ids
) {}