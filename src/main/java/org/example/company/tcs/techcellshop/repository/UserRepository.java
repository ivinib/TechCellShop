package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailUserIgnoreCase(String emailUser);
}
