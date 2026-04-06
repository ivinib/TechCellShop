package org.example.company.tcs.techcellshop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.company.tcs.techcellshop.config.SecurityConfig;
import org.example.company.tcs.techcellshop.dto.request.DeviceEnrollmentRequest;
import org.example.company.tcs.techcellshop.dto.request.DeviceUpdateRequest;
import org.example.company.tcs.techcellshop.dto.response.DeviceResponse;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.exception.ResourceNotFoundException;
import org.example.company.tcs.techcellshop.mapper.RequestMapper;
import org.example.company.tcs.techcellshop.mapper.ResponseMapper;
import org.example.company.tcs.techcellshop.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import(SecurityConfig.class)
@DisplayName("DeviceController")
class DeviceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private DeviceService deviceService;

    @MockitoBean
    private RequestMapper requestMapper;

    @MockitoBean
    private ResponseMapper responseMapper;

    private DeviceEnrollmentRequest validRequest;
    private DeviceUpdateRequest validUpdateRequest;
    private Device mockDevice;
    private DeviceResponse mockDeviceResponse;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validRequest = new DeviceEnrollmentRequest();
        validRequest.setNameDevice("Galaxy S24");
        validRequest.setDescriptionDevice("Samsung smartphone 256GB");
        validRequest.setDeviceType("SMARTPHONE");
        validRequest.setDeviceStorage("256GB");
        validRequest.setDeviceRam("8GB");
        validRequest.setDeviceColor("Black");
        validRequest.setDevicePrice(3999.90);
        validRequest.setDeviceStock(10);
        validRequest.setDeviceCondition("NEW");

        validUpdateRequest = new DeviceUpdateRequest();
        validUpdateRequest.setNameDevice("Galaxy S24");
        validUpdateRequest.setDescriptionDevice("Samsung smartphone 256GB");
        validUpdateRequest.setDeviceType("SMARTPHONE");
        validUpdateRequest.setDeviceStorage("256GB");
        validUpdateRequest.setDeviceRam("8GB");
        validUpdateRequest.setDeviceColor("Black");
        validUpdateRequest.setDevicePrice(3999.90);
        validUpdateRequest.setDeviceStock(10);
        validUpdateRequest.setDeviceCondition("NEW");

        mockDevice = new Device();
        mockDevice.setIdDevice(1L);
        mockDevice.setNameDevice("Galaxy S24");
        mockDevice.setDescriptionDevice("Samsung smartphone 256GB");
        mockDevice.setDeviceType("SMARTPHONE");
        mockDevice.setDeviceStorage("256GB");
        mockDevice.setDeviceRam("8GB");
        mockDevice.setDeviceColor("Black");
        mockDevice.setDevicePrice(3999.90);
        mockDevice.setDeviceStock(10);
        mockDevice.setDeviceCondition("NEW");

        mockDeviceResponse = new DeviceResponse(
                1L, "Galaxy S24", "Samsung smartphone 256GB", "SMARTPHONE",
                "256GB", "8GB", "Black", 3999.90, 10, "NEW"
        );
    }

    @Nested
    @DisplayName("POST /api/v1/devices")
    class SaveDevice {

        @Test
        @WithMockUser
        @DisplayName("Should return 201 when request is valid")
        void shouldReturn201_whenRequestIsValid() throws Exception {
            when(requestMapper.toDevice(any())).thenReturn(mockDevice);
            when(deviceService.saveDevice(any())).thenReturn(mockDevice);
            when(responseMapper.toDeviceResponse(any())).thenReturn(mockDeviceResponse);

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/devices/1")))
                    .andExpect(jsonPath("$.idDevice").value(1L))
                    .andExpect(jsonPath("$.nameDevice").value("Galaxy S24"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when device type is invalid")
        void shouldReturn400_whenDeviceTypeIsInvalid() throws Exception {
            validRequest.setDeviceType("PHONE");

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.deviceType").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when storage format is invalid")
        void shouldReturn400_whenStorageFormatIsInvalid() throws Exception {
            validRequest.setDeviceStorage("256");

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.deviceStorage").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when price is negative")
        void shouldReturn400_whenPriceIsNegative() throws Exception {
            validRequest.setDevicePrice(-1.0);

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.devicePrice").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when condition is invalid")
        void shouldReturn400_whenConditionIsInvalid() throws Exception {
            validRequest.setDeviceCondition("OLD");

            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.deviceCondition").exists());
        }

        @Test
        @DisplayName("Should return 403 when unauthenticated")
        void shouldReturn403_whenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices")
    class GetAllDevices {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with device list when authenticated")
        void shouldReturn200_withDeviceList_whenAuthenticated() throws Exception {
            when(deviceService.getAllDevices()).thenReturn(List.of(mockDevice));
            when(responseMapper.toDeviceResponseList(any())).thenReturn(List.of(mockDeviceResponse));

            mockMvc.perform(get("/api/v1/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nameDevice").value("Galaxy S24"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with empty list when no devices exist")
        void shouldReturn200_withEmptyList_whenNoDevicesExist() throws Exception {
            when(deviceService.getAllDevices()).thenReturn(List.of());
            when(responseMapper.toDeviceResponseList(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 403 when unauthenticated")
        void shouldReturn403_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/devices"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/{id}")
    class GetDeviceById {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when device is found")
        void shouldReturn200_whenDeviceIsFound() throws Exception {
            when(deviceService.getDeviceById(1L)).thenReturn(mockDevice);
            when(responseMapper.toDeviceResponse(any())).thenReturn(mockDeviceResponse);

            mockMvc.perform(get("/api/v1/devices/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idDevice").value(1L))
                    .andExpect(jsonPath("$.nameDevice").value("Galaxy S24"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when device is not found")
        void shouldReturn404_whenDeviceIsNotFound() throws Exception {
            when(deviceService.getDeviceById(99L))
                    .thenThrow(new ResourceNotFoundException("Device not found with id: 99"));

            mockMvc.perform(get("/api/v1/devices/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Device not found with id: 99"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/devices/{id}")
    class UpdateDevice {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when device is updated")
        void shouldReturn200_whenDeviceIsUpdated() throws Exception {
            DeviceResponse updatedResponse = new DeviceResponse(
                    1L, "Galaxy S24 Ultra", "Samsung smartphone 512GB", "SMARTPHONE",
                    "512GB", "12GB", "Black", 4999.90, 8, "NEW"
            );
            when(deviceService.updateDevice(eq(1L), any())).thenReturn(mockDevice);
            when(responseMapper.toDeviceResponse(any())).thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/devices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nameDevice").value("Galaxy S24 Ultra"))
                    .andExpect(jsonPath("$.devicePrice").value(4999.90));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when device is not found")
        void shouldReturn404_whenDeviceIsNotFound() throws Exception {
            when(deviceService.updateDevice(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Device not found with id: 99"));

            mockMvc.perform(put("/api/v1/devices/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Device not found with id: 99"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/devices/{id}")
    class PartialUpdateDevice {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when device is partially updated")
        void shouldReturn200_whenDeviceIsPartiallyUpdated() throws Exception {
            when(deviceService.updateDevice(eq(1L), any())).thenReturn(mockDevice);
            when(responseMapper.toDeviceResponse(any())).thenReturn(mockDeviceResponse);

            mockMvc.perform(patch("/api/v1/devices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idDevice").value(1L));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/devices/{id}")
    class DeleteDevice {

        @Test
        @WithMockUser
        @DisplayName("Should return 204 when device is deleted")
        void shouldReturn204_whenDeviceIsDeleted() throws Exception {
            doNothing().when(deviceService).deleteDevice(1L);

            mockMvc.perform(delete("/api/v1/devices/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when device is not found")
        void shouldReturn404_whenDeviceIsNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Device not found with id: 99"))
                    .when(deviceService).deleteDevice(99L);

            mockMvc.perform(delete("/api/v1/devices/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Device not found with id: 99"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());

        }
    }
}