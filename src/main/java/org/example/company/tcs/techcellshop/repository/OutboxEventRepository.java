package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e " +
            "WHERE e.status = :status AND e.nextAttemptAt <= :now " +
            "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingBatch(@Param("status") OutboxEventStatus status, @Param("now") Instant now, Pageable pageable);

    Page<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxEventStatus status, Pageable pageable);

    Optional<OutboxEvent> findByEventId(String eventId);

    @Modifying
    @Query("""
       update OutboxEvent e
          set e.status = 'PENDING',
              e.nextAttemptAt = :now,
              e.lastError = null
        where e.id in :ids
          and e.status = 'FAILED'
       """)
    int requeueFailedByIds(@Param("ids") List<Long> ids, @Param("now") Instant now);
}
