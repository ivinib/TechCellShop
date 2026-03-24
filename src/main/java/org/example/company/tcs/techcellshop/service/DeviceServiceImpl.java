package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService{

    private DeviceRepository deviceRepository;
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);

    DeviceServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
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
            return new RuntimeException("Device not found with id " + id);
        });
    }

    @Override
    public List<Device> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        log.info("Retrieved {} devices from the database", devices.size());
        return devices;
    }

    @Override
    public Device updateDevice(Long id, Device device) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("No device found with id {}", id);
                    return new ResourceNotFoundException("Device not found with id: " + id);
                });

        existingDevice.setNameDevice(device.getNameDevice());
        existingDevice.setDescriptionDevice(device.getDescriptionDevice());
        existingDevice.setDeviceType(device.getDeviceType());
        existingDevice.setDeviceStorage(device.getDeviceStorage());
        existingDevice.setDeviceRam(device.getDeviceRam());
        existingDevice.setDeviceColor(device.getDeviceColor());
        existingDevice.setDevicePrice(device.getDevicePrice());
        existingDevice.setDeviceStock(device.getDeviceStock());
        existingDevice.setDeviceCondition(device.getDeviceCondition());

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
}
