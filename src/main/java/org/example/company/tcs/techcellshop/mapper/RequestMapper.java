package org.example.company.tcs.techcellshop.mapper;

import org.example.company.tcs.techcellshop.controller.dto.request.DeviceEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.OrderEnrollmentRequest;
import org.example.company.tcs.techcellshop.controller.dto.request.UserEnrollmentRequest;
import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {
    public User toUser(UserEnrollmentRequest request) {
        User user = new User();
        user.setNameUser(request.getNameUser());
        user.setEmailUser(request.getEmailUser());
        user.setPasswordUser(request.getPasswordUser());
        user.setPhoneUser(request.getPhoneUser());
        user.setAddressUser(request.getAddressUser());
        user.setRoleUser(request.getRoleUser());
        return user;
    }

    public Device toDevice(DeviceEnrollmentRequest request) {
        Device device = new Device();
        device.setNameDevice(request.getNameDevice());
        device.setDescriptionDevice(request.getDescriptionDevice());
        device.setDeviceType(request.getDeviceType());
        device.setDeviceStorage(request.getDeviceStorage());
        device.setDeviceRam(request.getDeviceRam());
        device.setDeviceColor(request.getDeviceColor());
        device.setDevicePrice(request.getDevicePrice());
        device.setDeviceStock(request.getDeviceStock());
        device.setDeviceCondition(request.getDeviceCondition());
        return device;
    }

    public Order toOrder(OrderEnrollmentRequest request) {
        User user = new User();
        user.setIdUser(request.getIdUser());

        Device device = new Device();
        device.setIdDevice(request.getIdDevice());

        Order order = new Order();
        order.setUser(user);
        order.setDevice(device);
        order.setQuantityOrder(request.getQuantityOrder());
        order.setTotalPriceOrder(request.getTotalPriceOrder());
        order.setStatusOrder(request.getStatusOrder());
        order.setOrderDate(request.getOrderDate());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentStatus());
        return order;
    }
}
