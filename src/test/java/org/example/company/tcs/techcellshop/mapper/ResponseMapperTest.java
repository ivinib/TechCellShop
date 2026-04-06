package org.example.company.tcs.techcellshop.mapper;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.response.DeviceResponse;
import org.example.company.tcs.techcellshop.dto.response.OrderResponse;
import org.example.company.tcs.techcellshop.dto.response.UserResponse;
import org.example.company.tcs.techcellshop.util.OrderStatus;
import org.example.company.tcs.techcellshop.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ResponseMapperTest {

    private ResponseMapper responseMapper;
    private User user;
    private Device device;
    private Order order;

    @BeforeEach
    void setUp() {
        responseMapper = new ResponseMapper();

        user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");
        user.setPhoneUser("+55 11 90000-0001");
        user.setAddressUser("Rua das Flores, 123 - Sao Paulo - SP");
        user.setRoleUser("USER");

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

        order = new Order();
        order.setIdOrder(1L);
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(2);
        order.setTotalPriceOrder(7999.80);
        order.setStatus(OrderStatus.CREATED);
        order.setOrderDate("2026-03-24");
        order.setDeliveryDate("2026-03-31");
        order.setPaymentMethod("CREDIT_CARD");
        order.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Test
    void toUserResponse_shouldMapAllFieldsAndMaskEmail() {
        UserResponse result = responseMapper.toUserResponse(user);

        assertThat(result.idUser()).isEqualTo(1L);
        assertThat(result.nameUser()).isEqualTo("Ana Silva");
        assertThat(result.emailUserMasked()).isEqualTo("a***a@techcellshop.com");
        assertThat(result.phoneUser()).isEqualTo("+55 11 90000-0001");
        assertThat(result.addressUser()).isEqualTo("Rua das Flores, 123 - Sao Paulo - SP");
        assertThat(result.roleUser()).isEqualTo("USER");
    }

    @Test
    void toUserResponse_emailWithTwoCharLocalPart_shouldUseShortMaskFormat() {
        user.setEmailUser("ab@techcellshop.com");

        UserResponse result = responseMapper.toUserResponse(user);

        // local "ab" has length == 2, so: firstChar + "***@" + domain
        assertThat(result.emailUserMasked()).isEqualTo("a***@techcellshop.com");
    }

    @Test
    void toUserResponse_emailWithOneCharLocalPart_shouldUseShortMaskFormat() {
        user.setEmailUser("a@techcellshop.com");

        UserResponse result = responseMapper.toUserResponse(user);

        assertThat(result.emailUserMasked()).isEqualTo("a***@techcellshop.com");
    }

    @Test
    void toUserResponseList_shouldMapAllUsersInList() {
        List<UserResponse> result = responseMapper.toUserResponseList(List.of(user));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nameUser()).isEqualTo("Ana Silva");
        assertThat(result.get(0).emailUserMasked()).isEqualTo("a***a@techcellshop.com");
    }

    @Test
    void toDeviceResponse_shouldMapAllFields() {
        DeviceResponse result = responseMapper.toDeviceResponse(device);

        assertThat(result.idDevice()).isEqualTo(1L);
        assertThat(result.nameDevice()).isEqualTo("Galaxy S24");
        assertThat(result.descriptionDevice()).isEqualTo("Samsung smartphone 256GB");
        assertThat(result.deviceType()).isEqualTo("SMARTPHONE");
        assertThat(result.deviceStorage()).isEqualTo("256GB");
        assertThat(result.deviceRam()).isEqualTo("8GB");
        assertThat(result.deviceColor()).isEqualTo("Black");
        assertThat(result.devicePrice()).isEqualTo(3999.90);
        assertThat(result.deviceStock()).isEqualTo(10);
        assertThat(result.deviceCondition()).isEqualTo("NEW");
    }

    @Test
    void toDeviceResponseList_shouldMapAllDevicesInList() {
        List<DeviceResponse> result = responseMapper.toDeviceResponseList(List.of(device));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nameDevice()).isEqualTo("Galaxy S24");
    }

    @Test
    void toOrderResponse_shouldMapAllFieldsWithUserAndDeviceSummary() {
        OrderResponse result = responseMapper.toOrderResponse(order);

        assertThat(result.idOrder()).isEqualTo(1L);
        assertThat(result.quantityOrder()).isEqualTo(2);
        assertThat(result.totalPriceOrder()).isEqualTo(7999.80);
        assertThat(result.statusOrder()).isEqualTo("CREATED");
        assertThat(result.orderDate()).isEqualTo("2026-03-24");
        assertThat(result.deliveryDate()).isEqualTo("2026-03-31");
        assertThat(result.paymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(result.paymentStatus()).isEqualTo("PENDING");

        assertThat(result.user()).isNotNull();
        assertThat(result.user().idUser()).isEqualTo(1L);
        assertThat(result.user().nameUser()).isEqualTo("Ana Silva");
        assertThat(result.user().emailUserMasked()).isEqualTo("a***a@techcellshop.com");

        assertThat(result.device()).isNotNull();
        assertThat(result.device().idDevice()).isEqualTo(1L);
        assertThat(result.device().nameDevice()).isEqualTo("Galaxy S24");
        assertThat(result.device().devicePrice()).isEqualTo(3999.90);
    }

    @Test
    void toOrderResponse_whenUserIsNull_shouldReturnNullUserSummary() {
        order.setUser(null);

        OrderResponse result = responseMapper.toOrderResponse(order);

        assertThat(result.user()).isNull();
    }

    @Test
    void toOrderResponse_whenDeviceIsNull_shouldReturnNullDeviceSummary() {
        order.setDevice(null);

        OrderResponse result = responseMapper.toOrderResponse(order);

        assertThat(result.device()).isNull();
    }

    @Test
    void toOrderResponseList_shouldMapAllOrdersInList() {
        List<OrderResponse> result = responseMapper.toOrderResponseList(List.of(order));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).statusOrder()).isEqualTo("CREATED");
    }
}
