package org.example.company.tcs.techcellshop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.company.tcs.techcellshop.dto.request.DeviceEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.DeviceResponse;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Tag(name = "Device Management", description = "Endpoints for managing devices")
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

    @Operation(
            summary = "Enroll a device",
            description = "Saves a new device to the database, including it in the catalog",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device enrolled"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<DeviceResponse> saveDevice(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payload to enroll a new device",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Device payload example",
                                    value = """
                                            {
                                              "nameDevice": "Galaxy S24",
                                              "descriptionDevice": "Samsung smartphone 256GB",
                                              "deviceType": "SMARTPHONE",
                                              "deviceStorage": "256GB",
                                              "deviceRam": "8GB",
                                              "deviceColor": "Black",
                                              "devicePrice": 3999.90,
                                              "deviceStock": 10,
                                              "deviceCondition": "NEW"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody DeviceEnrollmentRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Device device = requestMapper.toDevice(request);
        Device savedDevice = deviceService.saveDevice(device);
        DeviceResponse response = responseMapper.toDeviceResponse(savedDevice);

        URI location = uriBuilder
                .path("/api/v1/devices/{id}")
                .buildAndExpand(savedDevice.getIdDevice())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "List all devices enrolled",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponse(responseCode = "200", description = "Devices returned")
    @GetMapping
    public ResponseEntity<Page<DeviceResponse>> getAllDevices(@PageableDefault(size = 20, sort = "idDevice") Pageable pageable) {
        Page<Device> devices = deviceService.getAllDevices(pageable);
        Page<DeviceResponse> response = devices.map(responseMapper::toDeviceResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Search for a specific device by its id",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device found"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id) {
        Device device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(device));
    }

    @Operation(
            summary = "Update a device",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        Device updatedDevice = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(updatedDevice));
    }

    @Operation(
            summary = "Update a device partially",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @PatchMapping("/{id}")
    public ResponseEntity<DeviceResponse> partiallyUpdateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        Device updatedDevice = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(responseMapper.toDeviceResponse(updatedDevice));
    }

    @Operation(
            summary = "Delete a specific device using its id",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
