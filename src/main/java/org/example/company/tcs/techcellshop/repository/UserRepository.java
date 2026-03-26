package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailUserIgnoreCase(String emailUser);

    Optional<User> findByEmailUserIgnoreCase(String emailUser);
}
