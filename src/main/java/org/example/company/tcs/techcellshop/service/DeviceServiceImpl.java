package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Device> saveDevice(Device device) {
        try {
            Device savedDevice = deviceRepository.save(device);
            log.info("Device saved successfully");
            return ResponseEntity.ok(savedDevice);
        } catch (Exception e) {
            log.error("An error occurred while trying to save the device. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Device> getDeviceById(Long id) {
        try {
            Device device = deviceRepository.findById(id).orElse(null);
            if (device == null) {
                log.info("No device found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            log.info("Returning device with id {}", id);
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            log.error("An error occurred while trying to get the device by id. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<Device>> getAllDevices() {
        try{
            List<Device> devices = deviceRepository.findAll();
            if (devices.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            log.info("Returning all devices. Total of devices found: {}", devices.size());
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("An error occurred while trying to get all devices. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Device> updateDevice(Long id, Device device) {
        try {
            Device existingDevice = deviceRepository.findById(id).orElse(null);
            if (existingDevice == null) {
                log.info("No device found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            existingDevice.setNameDevice(device.getNameDevice());
            existingDevice.setDevicePrice(device.getDevicePrice());
            existingDevice.setDeviceStock(device.getDeviceStock());
            Device updatedDevice = deviceRepository.save(existingDevice);
            log.info("Device with id {} updated successfully", id);
            return ResponseEntity.ok(updatedDevice);
        } catch (Exception e) {
            log.error("An error occurred while trying to update the device. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> deleteDevice(Long id) {
        try{
            Device existingDevice = deviceRepository.findById(id).orElse(null);
            if (existingDevice == null) {
                log.info("No device found with id {}", id);
                return ResponseEntity.notFound().build();
            }
            deviceRepository.delete(existingDevice);
            log.info("Device with id {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("An error occurred while trying to delete the device. Error:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
