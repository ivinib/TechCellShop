package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface DeviceService {
    Device saveDevice(Device device);
    Device getDeviceById(Long id);
    Page<Device> getAllDevices(Pageable pageable);
    Device updateDevice(Long id, DeviceUpdateRequest request);
    void deleteDevice(Long id);
    void reserveStock(Long deviceId, Integer quantity);
    void releaseStock(Long deviceId, Integer quantity);
}
