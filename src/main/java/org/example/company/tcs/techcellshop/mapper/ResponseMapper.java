package org.example.company.tcs.techcellshop.mapper;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResponseMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getIdUser(),
                user.getNameUser(),
                maskEmail(user.getEmailUser()),
                user.getPhoneUser(),
                user.getAddressUser(),
                user.getRoleUser()
        );
    }

    public List<UserResponse> toUserResponseList(List<User> users) {
        return users.stream().map(this::toUserResponse).toList();
    }

    public DeviceResponse toDeviceResponse(Device device) {
        return new DeviceResponse(
                device.getIdDevice(),
                device.getNameDevice(),
                device.getDescriptionDevice(),
                device.getDeviceType(),
                device.getDeviceStorage(),
                device.getDeviceRam(),
                device.getDeviceColor(),
                device.getDevicePrice(),
                device.getDeviceStock(),
                device.getDeviceCondition()
        );
    }

    public List<DeviceResponse> toDeviceResponseList(List<Device> devices) {
        return devices.stream().map(this::toDeviceResponse).toList();
    }

    public OrderResponse toOrderResponse(Order order) {
        UserSummaryResponse userSummary = buildUserSummary(order);
        DeviceSummaryResponse deviceSummary = buildDeviceSummary(order);

        return new OrderResponse(
                order.getIdOrder(),
                userSummary,
                deviceSummary,
                order.getQuantityOrder(),
                order.getTotalPriceOrder(),
                order.getStatus(),
                order.getOrderDate(),
                order.getDeliveryDate(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getCouponCode(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getCanceledReason()
        );
    }

    public List<OrderResponse> toOrderResponseList(List<Order> orders) {
        return orders.stream().map(this::toOrderResponse).toList();
    }

    private UserSummaryResponse buildUserSummary(Order order) {
        if (order.getUserIdSnapshot() != null
                || order.getUserNameSnapshot() != null
                || order.getUserEmailSnapshot() != null) {
            return new UserSummaryResponse(
                    order.getUserIdSnapshot(),
                    order.getUserNameSnapshot(),
                    maskEmail(order.getUserEmailSnapshot())
            );
        }

        User user = order.getUser();
        if (user == null) {
            return null;
        }

        return new UserSummaryResponse(
                user.getIdUser(),
                user.getNameUser(),
                maskEmail(user.getEmailUser())
        );
    }

    private DeviceSummaryResponse buildDeviceSummary(Order order) {
        if (order.getDeviceIdSnapshot() != null
                || order.getDeviceNameSnapshot() != null
                || order.getUnitPriceSnapshot() != null) {
            return new DeviceSummaryResponse(
                    order.getDeviceIdSnapshot(),
                    order.getDeviceNameSnapshot(),
                    order.getUnitPriceSnapshot()
            );
        }

        Device device = order.getDevice();
        if (device == null) {
            return null;
        }

        return new DeviceSummaryResponse(
                device.getIdDevice(),
                device.getNameDevice(),
                device.getDevicePrice()
        );
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }

        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}
