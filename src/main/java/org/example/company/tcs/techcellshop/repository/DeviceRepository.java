package org.example.company.tcs.techcellshop.repository;

import org.example.company.tcs.techcellshop.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
}
