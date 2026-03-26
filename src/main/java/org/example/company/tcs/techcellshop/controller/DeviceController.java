package org.example.company.tcs.techcellshop.controller;

import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.controller.dto.request.DeviceEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.controller.dto.response.DeviceResponse;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
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
    public ResponseEntity<DeviceResponse> saveDevice(@Valid @RequestBody DeviceEnrollmentRequest request, UriComponentsBuilder uriBuilder) {
        Device device = requestMapper.toDevice(request);
        Device savedDevice = deviceService.saveDevice(device);
        DeviceResponse response = responseMapper.toDeviceResponse(savedDevice);

        URI location = uriBuilder
                .path("/api/v1/devices/{id}")
                .buildAndExpand(savedDevice.getIdDevice())
                .toUri();

        return ResponseEntity.created(location).body(response);
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
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        Device updatedDevice = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(updatedDevice));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponse> partiallyUpdateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        Device updatedDevice = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(updatedDevice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
