package org.example.company.tcs.techcellshop.controller;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.service.DeviceServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceServiceImpl deviceService;

    DeviceController(DeviceServiceImpl deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<Device> saveDevice(@RequestBody Device device) {
        return deviceService.saveDevice(device);
    }

    @GetMapping
    public ResponseEntity<List<Device>> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        return deviceService.updateDevice(id, device);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        try {
            deviceService.getDeviceById(id);
            deviceService.deleteDevice(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
