package org.example.company.tcs.techcellshop.service.impl;

import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.dto.OutboxEventResponseDto;
import org.example.company.tcs.techcellshop.dto.RequeueResultDto;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.service.OutboxAdminService;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutboxAdminServiceImpl implements OutboxAdminService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxAdminServiceImpl(OutboxEventRepository outboxEventRepository){
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OutboxEventResponseDto> listFailed(int page, int size) {
        Page<OutboxEvent> failedPage = outboxEventRepository
                .findByStatusOrderByCreatedAtDesc(OutboxEventStatus.FAILED, PageRequest.of(page, size));

        List<OutboxEventResponseDto> content = failedPage.getContent()
                .stream()
                .map(this::toDto)
                .toList();

        return new PageImpl<>(content, failedPage.getPageable(), failedPage.getTotalElements());
    }

    @Override
    @Transactional
    public RequeueResultDto requeueFailed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new RequeueResultDto(
                    List.of(),
                    0,
                    List.of(),
                    List.of(),
                    "No outbox event ids were informed"
            );
        }

        List<Long> requestedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (requestedIds.isEmpty()) {
            return new RequeueResultDto(
                    List.of(),
                    0,
                    List.of(),
                    List.of(),
                    "No valid outbox event ids were informed"
            );
        }

        List<OutboxEvent> foundEvents = outboxEventRepository.findAllById(requestedIds);

        Map<Long, OutboxEvent> foundById = foundEvents.stream()
                .collect(Collectors.toMap(OutboxEvent::getId, Function.identity()));

        List<Long> notFoundIds = requestedIds.stream()
                .filter(id -> !foundById.containsKey(id))
                .toList();

        List<Long> ignoredIds = new ArrayList<>();
        List<OutboxEvent> eventsToRequeue = new ArrayList<>();

        for (Long id : requestedIds) {
            OutboxEvent event = foundById.get(id);

            if (event == null) {
                continue;
            }

            if (event.getStatus() != OutboxEventStatus.FAILED) {
                ignoredIds.add(id);
                continue;
            }

            resetForRetry(event);
            eventsToRequeue.add(event);
        }

        if (!eventsToRequeue.isEmpty()) {
            outboxEventRepository.saveAll(eventsToRequeue);
        }

        String message = "Requeue finished: " + eventsToRequeue.size()
                + " event(s) moved back to PENDING";

        return new RequeueResultDto(
                requestedIds,
                eventsToRequeue.size(),
                notFoundIds,
                ignoredIds,
                message
        );
    }

    @Override
    @Transactional
    public RequeueResultDto requeueOne(Long id) {
        return requeueFailed(List.of(id));
    }

    private void resetForRetry(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now());
        event.setLastError(null);
        event.setSentAt(null);
    }

    private OutboxEventResponseDto toDto(OutboxEvent event) {
        return new OutboxEventResponseDto(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getStatus(),
                event.getAttempts(),
                event.getCreatedAt(),
                event.getNextAttemptAt(),
                event.getSentAt(),
                event.getLastError()
        );
    }
}
