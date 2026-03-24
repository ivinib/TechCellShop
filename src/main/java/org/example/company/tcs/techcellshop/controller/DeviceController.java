package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.request.DeviceEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.DeviceResponse;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final RequestMapper requestMapper;
    private final ResponseMapper responseMapper;

    DeviceController(DeviceService deviceService, RequestMapper requestMapper, ResponseMapper responseMapper) {
        this.deviceService = deviceService;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> saveDevice(@Valid @RequestBody DeviceEnrollmentRequest request) {
        Device device = requestMapper.toDevice(request);
        Device savedDevice = deviceService.saveDevice(device);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(savedDevice));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        List<Device> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(responseMapper.toDeviceResponseList(devices));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id) {
        Device device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(device));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        Device updatedDevice = deviceService.updateDevice(id, device);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(updatedDevice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
