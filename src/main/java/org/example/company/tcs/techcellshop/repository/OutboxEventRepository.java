package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.OutboxEvent;
import org.example.company.tcs.techcellshop.util.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e " +
            "WHERE e.status = :status AND e.nextAttemptAt <= :now " +
            "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingBatch(@Param("status") OutboxEventStatus status, @Param("now") Instant now, Pageable pageable);
}
