package org.example.company.tcs.techcellshop.mapper;

import org.example.company.tcs.techcellshop.controller.dto.request.DeviceEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RequestMapperTest {

    private RequestMapper requestMapper;

    @BeforeEach
    void setUp() {
        requestMapper = new RequestMapper();
    }

    @Test
    void toUser_shouldMapAllFieldsFromRequest() {
        UserEnrollmentRequest request = new UserEnrollmentRequest();
        request.setNameUser("Ana Silva");
        request.setEmailUser("ana@techcellshop.com");
        request.setPasswordUser("senha123");
        request.setPhoneUser("+55 11 90000-0001");
        request.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        request.setRoleUser("USER");

        User result = requestMapper.toUser(request);

        assertThat(result.getNameUser()).isEqualTo("Ana Silva");
        assertThat(result.getEmailUser()).isEqualTo("ana@techcellshop.com");
        assertThat(result.getPasswordUser()).isEqualTo("senha123");
        assertThat(result.getPhoneUser()).isEqualTo("+55 11 90000-0001");
        assertThat(result.getAddressUser()).isEqualTo("Rua das Flores, 123 - Sao Paulo - SP");
        assertThat(result.getRoleUser()).isEqualTo("USER");
        assertThat(result.getIdUser()).isNull();
    }

    @Test
    void toDevice_shouldMapAllFieldsFromRequest() {
        DeviceEnrollmentRequest request = new DeviceEnrollmentRequest();
        request.setNameDevice("Galaxy S24");
        request.setDescriptionDevice("Samsung smartphone 256GB");
        request.setDeviceType("SMARTPHONE");
        request.setDeviceStorage("256GB");
        request.setDeviceRam("8GB");
        request.setDeviceColor("Black");
        request.setDevicePrice(3999.90);
        request.setDeviceStock(10);
        request.setDeviceCondition("NEW");

        Device result = requestMapper.toDevice(request);

        assertThat(result.getNameDevice()).isEqualTo("Galaxy S24");
        assertThat(result.getDescriptionDevice()).isEqualTo("Samsung smartphone 256GB");
        assertThat(result.getDeviceType()).isEqualTo("SMARTPHONE");
        assertThat(result.getDeviceStorage()).isEqualTo("256GB");
        assertThat(result.getDeviceRam()).isEqualTo("8GB");
        assertThat(result.getDeviceColor()).isEqualTo("Black");
        assertThat(result.getDevicePrice()).isEqualTo(3999.90);
        assertThat(result.getDeviceStock()).isEqualTo(10);
        assertThat(result.getDeviceCondition()).isEqualTo("NEW");
        assertThat(result.getIdDevice()).isNull();
    }

    @Test
    void toOrder_shouldMapAllFieldsAndSetUserAndDeviceById() {
        OrderEnrollmentRequest request = new OrderEnrollmentRequest();
        request.setIdUser(1L);
        request.setIdDevice(2L);
        request.setQuantityOrder(1);
        request.setTotalPriceOrder(3999.90);
        request.setStatusOrder("CREATED");
        request.setOrderDate("2026-03-24");
        request.setDeliveryDate("2026-03-31");
        request.setPaymentMethod("PIX");
        request.setPaymentStatus("PAID");

        Order result = requestMapper.toOrder(request);

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getIdUser()).isEqualTo(1L);
        assertThat(result.getDevice()).isNotNull();
        assertThat(result.getDevice().getIdDevice()).isEqualTo(2L);
        assertThat(result.getQuantityOrder()).isEqualTo(1);
        assertThat(result.getTotalPriceOrder()).isEqualTo(3999.90);
        assertThat(result.getStatusOrder()).isEqualTo("CREATED");
        assertThat(result.getOrderDate()).isEqualTo("2026-03-24");
        assertThat(result.getDeliveryDate()).isEqualTo("2026-03-31");
        assertThat(result.getPaymentMethod()).isEqualTo("PIX");
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        assertThat(result.getIdOrder()).isNull();
    }
}
