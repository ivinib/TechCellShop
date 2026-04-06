package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;

import java.util.List;

public interface DeviceService {
    Device saveDevice(Device device);
    Device getDeviceById(Long id);
    List<Device> getAllDevices();
    Device updateDevice(Long id, DeviceUpdateRequest request);
    void deleteDevice(Long id);
    void reserveStock(Long deviceId, Integer quantity);
    void releaseStock(Long deviceId, Integer quantity);
}
