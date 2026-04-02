package org.example.company.tcs.techcellshop.repository;

import jakarta.persistence.LockModeType;
import org.example.company.tcs.techcellshop.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)")
    Optional<Coupon> findByCodeIgnoreCaseForUpdate(String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE tb_coupon
               SET used_count_coupon = COALESCE(used_count_coupon, 0) + 1
             WHERE LOWER(code_coupon) = LOWER(:code)
               AND (max_uses_coupon IS NULL OR COALESCE(used_count_coupon, 0) < max_uses_coupon)
            """, nativeQuery = true)
    int incrementUsageIfAvailable(@Param("code") String code);
}
