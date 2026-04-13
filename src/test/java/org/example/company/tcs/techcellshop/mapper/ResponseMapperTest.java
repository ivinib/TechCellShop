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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ResponseMapperTest {


    private final ResponseMapper responseMapper = new ResponseMapper();

    @Test
    @DisplayName("toOrderResponse should prefer snapshot fields when present")
    void toOrderResponse_shouldPreferSnapshotFieldsWhenPresent() {
        User user = new User();
        user.setIdUser(99L);
        user.setNameUser("Different User");
        user.setEmailUser("different@techcellshop.com");

        Device device = new Device();
        device.setIdDevice(88L);
        device.setNameDevice("Different Device");
        device.setDevicePrice(new BigDecimal("9999.99"));

        Order order = new Order();
        order.setIdOrder(1L);
        order.setUser(user);
        order.setDevice(device);

        order.setUserIdSnapshot(1L);
        order.setUserNameSnapshot("Ana Silva");
        order.setUserEmailSnapshot("ana@techcellshop.com");

        order.setDeviceIdSnapshot(1L);
        order.setDeviceNameSnapshot("Galaxy S24");
        order.setUnitPriceSnapshot(new BigDecimal("3999.90"));

        order.setQuantityOrder(1);
        order.setTotalPriceOrder(new BigDecimal("3999.90"));
        order.setStatus(OrderStatus.CREATED);
        order.setOrderDate("2026-04-01");
        order.setDeliveryDate("2026-04-06");
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setFinalAmount(new BigDecimal("3999.90"));

        OrderResponse response = responseMapper.toOrderResponse(order);

        assertThat(response.user()).isNotNull();
        assertThat(response.user().idUser()).isEqualTo(1L);
        assertThat(response.user().nameUser()).isEqualTo("Ana Silva");
        assertThat(response.user().emailUserMasked()).isEqualTo("a***a@techcellshop.com");

        assertThat(response.device()).isNotNull();
        assertThat(response.device().idDevice()).isEqualTo(1L);
        assertThat(response.device().nameDevice()).isEqualTo("Galaxy S24");
        assertThat(response.device().devicePrice()).isEqualByComparingTo("3999.90");
    }

    @Test
    @DisplayName("toOrderResponse should fallback to related entities when snapshots are absent")
    void toOrderResponse_shouldFallbackToRelationsWhenSnapshotsAreAbsent() {
        User user = new User();
        user.setIdUser(1L);
        user.setNameUser("Ana Silva");
        user.setEmailUser("ana@techcellshop.com");

        Device device = new Device();
        device.setIdDevice(1L);
        device.setNameDevice("Galaxy S24");
        device.setDevicePrice(new BigDecimal("3999.90"));

        Order order = new Order();
        order.setIdOrder(1L);
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(new BigDecimal("3999.90"));
        order.setStatus(OrderStatus.CREATED);
        order.setOrderDate("2026-04-01");
        order.setDeliveryDate("2026-04-06");
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setFinalAmount(new BigDecimal("3999.90"));

        OrderResponse response = responseMapper.toOrderResponse(order);

        assertThat(response.user()).isNotNull();
        assertThat(response.user().idUser()).isEqualTo(1L);
        assertThat(response.user().nameUser()).isEqualTo("Ana Silva");
        assertThat(response.user().emailUserMasked()).isEqualTo("a***a@techcellshop.com");

        assertThat(response.device()).isNotNull();
        assertThat(response.device().idDevice()).isEqualTo(1L);
        assertThat(response.device().nameDevice()).isEqualTo("Galaxy S24");
        assertThat(response.device().devicePrice()).isEqualByComparingTo("3999.90");
    }

    @Test
    @DisplayName("toOrderResponse should return null summaries when neither snapshots nor relations exist")
    void toOrderResponse_shouldReturnNullSummariesWhenNoSourceExists() {
        Order order = new Order();
        order.setIdOrder(1L);
        order.setQuantityOrder(1);
        order.setTotalPriceOrder(new BigDecimal("3999.90"));
        order.setStatus(OrderStatus.CREATED);
        order.setOrderDate("2026-04-01");
        order.setDeliveryDate("2026-04-06");
        order.setPaymentMethod("PIX");
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setFinalAmount(new BigDecimal("3999.90"));

        OrderResponse response = responseMapper.toOrderResponse(order);

        assertThat(response.user()).isNull();
        assertThat(response.device()).isNull();
    }
}
