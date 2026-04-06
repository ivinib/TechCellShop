package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.dto.OutboxEventResponseDto;
import org.example.company.tcs.techcellshop.dto.RequeueResultDto;
import org.example.company.tcs.techcellshop.repository.OutboxEventRepository;
import org.example.company.tcs.techcellshop.service.impl.OutboxAdminServiceImpl;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxAdminServiceImpl")
class OutboxAdminServiceImplTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxAdminServiceImpl service;

    @Nested
    @DisplayName("listFailed")
    class ListFailed {

        @Test
        @DisplayName("should return page of failed outbox events")
        void shouldReturnFailedPage() {
            OutboxEvent failedEvent = new OutboxEvent();
            failedEvent.setId(1L);
            failedEvent.setEventId("evt-001");
            failedEvent.setStatus(OutboxEventStatus.FAILED);
            failedEvent.setAttempts(5);

            Page<OutboxEvent> failedPage = new PageImpl<>(List.of(failedEvent));

            when(outboxEventRepository.findByStatusOrderByCreatedAtDesc(
                    OutboxEventStatus.FAILED, PageRequest.of(0, 20)))
                    .thenReturn(failedPage);

            Page<OutboxEventResponseDto> result = service.listFailed(0, 20);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(0).status()).isEqualTo(OutboxEventStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("requeueFailed")
    class RequeueFailed {

        @Test
        @DisplayName("should requeue only failed events")
        void shouldRequeueOnlyFailed() {
            OutboxEvent failed = new OutboxEvent();
            failed.setId(1L);
            failed.setEventId("evt-001");
            failed.setStatus(OutboxEventStatus.FAILED);
            failed.setAttempts(5);
            failed.setLastError("broker unavailable");
            failed.setCreatedAt(Instant.now());

            OutboxEvent sent = new OutboxEvent();
            sent.setId(2L);
            sent.setStatus(OutboxEventStatus.SENT);

            when(outboxEventRepository.findAllById(List.of(1L, 2L, 99L)))
                    .thenReturn(List.of(failed, sent));
            when(outboxEventRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RequeueResultDto result = service.requeueFailed(List.of(1L, 2L, 99L));

            assertThat(result.requeuedCount()).isEqualTo(1);
            assertThat(result.notFoundIds()).containsExactly(99L);
            assertThat(result.ignoredIds()).containsExactly(2L);

            assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(failed.getAttempts()).isZero();
            assertThat(failed.getLastError()).isNull();
            assertThat(failed.getSentAt()).isNull();
        }

        @Test
        @DisplayName("should return empty result for null ids")
        void shouldReturnEmptyForNullIds() {
            RequeueResultDto result = service.requeueFailed(null);

            assertThat(result.requeuedCount()).isZero();
            assertThat(result.requestedIds()).isEmpty();
        }

        @Test
        @DisplayName("should deduplicate requested ids")
        void shouldDeduplicateIds() {
            OutboxEvent event = new OutboxEvent();
            event.setId(1L);
            event.setStatus(OutboxEventStatus.FAILED);

            when(outboxEventRepository.findAllById(List.of(1L)))
                    .thenReturn(List.of(event));
            when(outboxEventRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RequeueResultDto result = service.requeueFailed(List.of(1L, 1L, 1L));

            assertThat(result.requeuedCount()).isEqualTo(1);

            verify(outboxEventRepository).findAllById(List.of(1L));
        }
    }

    @Nested
    @DisplayName("requeueOne")
    class RequeueOne {

        @Test
        @DisplayName("should requeue single event")
        void shouldRequeueSingleEvent() {
            OutboxEvent event = new OutboxEvent();
            event.setId(5L);
            event.setStatus(OutboxEventStatus.FAILED);

            when(outboxEventRepository.findAllById(List.of(5L)))
                    .thenReturn(List.of(event));
            when(outboxEventRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RequeueResultDto result = service.requeueOne(5L);

            assertThat(result.requeuedCount()).isEqualTo(1);
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(event.getAttempts()).isZero();
        }
    }
}