package org.example.company.tcs.techcellshop.service;

import org.example.company.tcs.techcellshop.controller.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.service.impl.DeviceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDescriptionDevice("Samsung smartphone 256GB");
        device.setDeviceType("SMARTPHONE");
        device.setDeviceStorage("256GB");
        device.setDeviceRam("8GB");
        device.setDeviceColor("Black");
        device.setDevicePrice(3999.90);
        device.setDeviceStock(10);
        device.setDeviceCondition("NEW");
    }

    @Test
    void saveDevice_shouldSaveAndReturnDevice() {
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        Device result = deviceService.saveDevice(device);

        assertThat(result.getIdDevice()).isEqualTo(1L);
        assertThat(result.getNameDevice()).isEqualTo("Galaxy S24");
        verify(deviceRepository).save(device);
    }

    @Test
    void getDeviceById_whenDeviceExists_shouldReturnDevice() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        Device result = deviceService.getDeviceById(1L);

        assertThat(result.getIdDevice()).isEqualTo(1L);
        assertThat(result.getNameDevice()).isEqualTo("Galaxy S24");
    }

    // NOTE: getDeviceById throws RuntimeException instead of ResourceNotFoundException.
    // This is inconsistent with updateDevice/deleteDevice — consider fixing to ResourceNotFoundException.
    @Test
    void getDeviceById_whenDeviceNotFound_shouldThrowRuntimeException() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Device not found with id 99");
    }

    @Test
    void getAllDevices_shouldReturnAllDevices() {
        when(deviceRepository.findAll()).thenReturn(List.of(device));

        List<Device> result = deviceService.getAllDevices();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getNameDevice()).isEqualTo("Galaxy S24");
    }

    @Test
    void getAllDevices_whenEmpty_shouldReturnEmptyList() {
        when(deviceRepository.findAll()).thenReturn(List.of());

        List<Device> result = deviceService.getAllDevices();

        assertThat(result).isEmpty();
    }

    @Test
    void updateDevice_whenDeviceExists_shouldUpdateAllFieldsAndReturn() {
        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest();
        updateRequest.setNameDevice("Galaxy S24 Ultra");
        updateRequest.setDescriptionDevice("Updated description");
        updateRequest.setDeviceType("SMARTPHONE");
        updateRequest.setDeviceStorage("512GB");
        updateRequest.setDeviceRam("12GB");
        updateRequest.setDeviceColor("Silver");
        updateRequest.setDevicePrice(4999.90);
        updateRequest.setDeviceStock(5);
        updateRequest.setDeviceCondition("NEW");

        Device updatedDevice = new Device();
        updatedDevice.setIdDevice(1L);
        updatedDevice.setNameDevice("Galaxy S24 Ultra");
        updatedDevice.setDescriptionDevice("Updated description");
        updatedDevice.setDeviceType("SMARTPHONE");
        updatedDevice.setDeviceStorage("512GB");
        updatedDevice.setDeviceRam("12GB");
        updatedDevice.setDeviceColor("Silver");
        updatedDevice.setDevicePrice(4999.90);
        updatedDevice.setDeviceStock(5);
        updatedDevice.setDeviceCondition("NEW");

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(updatedDevice);

        Device result = deviceService.updateDevice(1L, updateRequest);

        assertThat(result.getNameDevice()).isEqualTo("Galaxy S24 Ultra");
        assertThat(result.getDevicePrice()).isEqualTo(4999.90);
        assertThat(result.getDeviceStock()).isEqualTo(5);
        assertThat(result.getDeviceStorage()).isEqualTo("512GB");
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void updateDevice_whenDeviceNotFound_shouldThrowResourceNotFoundException() {
        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest();
        updateRequest.setNameDevice("Galaxy S24 Ultra");
        updateRequest.setDescriptionDevice("Updated description");
        updateRequest.setDeviceType("SMARTPHONE");
        updateRequest.setDeviceStorage("512GB");
        updateRequest.setDeviceRam("12GB");
        updateRequest.setDeviceColor("Silver");
        updateRequest.setDevicePrice(4999.90);
        updateRequest.setDeviceStock(5);
        updateRequest.setDeviceCondition("NEW");

        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.updateDevice(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found with id: 99");
    }

    @Test
    void deleteDevice_whenDeviceExists_shouldCallRepositoryDelete() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        deviceService.deleteDevice(1L);

        verify(deviceRepository).delete(device);
    }

    @Test
    void deleteDevice_whenDeviceNotFound_shouldThrowResourceNotFoundExceptionAndNeverDelete() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.deleteDevice(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Device not found with id: 99");

        verify(deviceRepository, never()).delete(any());
    }
}