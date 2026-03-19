package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Device;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface DeviceService {
    ResponseEntity<Device> saveDevice(Device device);

    ResponseEntity<Device> getDeviceById(Long id);

    ResponseEntity<List<Device>> getAllDevices();

    ResponseEntity<Device> updateDevice(Long id, Device device);

    ResponseEntity<Void> deleteDevice(Long id);
}
