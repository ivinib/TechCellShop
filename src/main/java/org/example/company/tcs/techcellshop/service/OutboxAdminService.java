package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.dto.OutboxEventResponseDto;
import org.example.company.tcs.techcellshop.dto.RequeueResultDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OutboxAdminService {
    Page<OutboxEventResponseDto> listFailed(int page, int size);
    RequeueResultDto requeueFailed(List<Long> ids);
    RequeueResultDto requeueOne(Long id);
}
