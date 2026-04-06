package org.example.company.tcs.techcellshop.dto;

import java.util.List;

public record RequeueResultDto(
        List<Long> requestedIds,
        int requeuedCount,
        List<Long> notFoundIds,
        List<Long> ignoredIds,
        String message
) {}
