package org.example.company.tcs.techcellshop.service.impl;

import jakarta.transaction.Transactional;
import org.example.company.tcs.techcellshop.controller.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.exception.InsufficientStockException;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);
    private final RequestMapper requestMapper;

    DeviceServiceImpl(DeviceRepository deviceRepository, RequestMapper requestMapper) {
        this.deviceRepository = deviceRepository;
        this.requestMapper = requestMapper;
    }

    @Override
    public Device saveDevice(Device device) {
        Device savedDevice = deviceRepository.save(device);
        log.info("Device with id {} saved successfully", savedDevice.getIdDevice());
        return savedDevice;
    }

    @Override
    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id).orElseThrow(() -> {
            log.info("No device found with id {}", id);
            return new ResourceNotFoundException("Device not found with id " + id);
        });
    }

    @Override
    public List<Device> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        log.info("Retrieved {} devices from the database", devices.size());
        return devices;
    }

    @Override
    public Device updateDevice(Long id, DeviceUpdateRequest request) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No device found with id {}", id);
                    return new ResourceNotFoundException("Device not found with id: " + id);
                });

        requestMapper.updateDevice(existingDevice, request);
        Device updatedDevice = deviceRepository.save(existingDevice);
        log.info("Device with id {} updated successfully", id);
        return updatedDevice;
    }

    @Override
    public void deleteDevice(Long id) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No device found with id {}", id);
                    return new ResourceNotFoundException("Device not found with id: " + id);
                });

        deviceRepository.delete(existingDevice);
        log.info("Device with id {} deleted successfully", id);
    }

    @Transactional
    @Override
    public void reserveStock(Long deviceId, Integer quantity) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        if (device.getDeviceStock() < quantity) {
            throw new InsufficientStockException(String.format("Insufficient stock for device id %d. Available: %d, Requested: %d", deviceId, device.getDeviceStock(), quantity));
        }

        device.setDeviceStock(device.getDeviceStock() - quantity);
        deviceRepository.save(device);
    }

    @Transactional
    @Override
    public void releaseStock(Long deviceId, Integer quantity) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        device.setDeviceStock(device.getDeviceStock() + quantity);
        deviceRepository.save(device);
    }
}
