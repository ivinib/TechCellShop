package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.OrderIdempondency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderIdempondencyRepository extends JpaRepository<OrderIdempondency, Long> {
    Optional<OrderIdempondency> findByIdempotencyKey(String idempotencyKey);
}
