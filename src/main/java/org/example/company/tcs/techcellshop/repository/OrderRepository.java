package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser_EmailUserIgnoreCase(String emailUser, Pageable pageable);
}
