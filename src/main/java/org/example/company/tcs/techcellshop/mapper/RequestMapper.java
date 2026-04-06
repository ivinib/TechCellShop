package org.example.company.tcs.techcellshop.mapper;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.dto.request.*;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {
    public User toUser(UserEnrollmentRequest request) {
        User user = new User();
        applyUserCommonFields(
                user,
                request.getNameUser(),
                request.getEmailUser(),
                request.getPhoneUser(),
                request.getAddressUser(),
                request.getRoleUser()
        );
        user.setPasswordUser(request.getPasswordUser());
        return user;
    }

    public void updateUser(User user, UserUpdateRequest request) {
        applyUserCommonFields(
                user,
                request.getNameUser(),
                request.getEmailUser(),
                request.getPhoneUser(),
                request.getAddressUser(),
                request.getRoleUser()
        );
    }

    public Device toDevice(DeviceEnrollmentRequest request) {
        Device device = new Device();
        applyDeviceFields(
                device,
                request.getNameDevice(),
                request.getDescriptionDevice(),
                request.getDeviceType(),
                request.getDeviceStorage(),
                request.getDeviceRam(),
                request.getDeviceColor(),
                request.getDevicePrice(),
                request.getDeviceStock(),
                request.getDeviceCondition()
        );
        return device;
    }

    public void updateDevice(Device device, DeviceUpdateRequest request) {
        applyDeviceFields(
                device,
                request.getNameDevice(),
                request.getDescriptionDevice(),
                request.getDeviceType(),
                request.getDeviceStorage(),
                request.getDeviceRam(),
                request.getDeviceColor(),
                request.getDevicePrice(),
                request.getDeviceStock(),
                request.getDeviceCondition()
        );
    }

    public void updateOrder(Order order, OrderUpdateRequest request) {
        order.setQuantityOrder(request.getQuantityOrder());
        order.setStatus(request.getStatusOrder());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setPaymentStatus(request.getPaymentStatus());
    }

    private void applyUserCommonFields(
            User user,
            String nameUser,
            String emailUser,
            String phoneUser,
            String addressUser,
            String roleUser
    ) {
        user.setNameUser(nameUser);
        user.setEmailUser(emailUser);
        user.setPhoneUser(phoneUser);
        user.setAddressUser(addressUser);
        user.setRoleUser(roleUser);
    }

    private void applyDeviceFields(
            Device device,
            String nameDevice,
            String descriptionDevice,
            String deviceType,
            String deviceStorage,
            String deviceRam,
            String deviceColor,
            Double devicePrice,
            Integer deviceStock,
            String deviceCondition
    ) {
        device.setNameDevice(nameDevice);
        device.setDescriptionDevice(descriptionDevice);
        device.setDeviceType(deviceType);
        device.setDeviceStorage(deviceStorage);
        device.setDeviceRam(deviceRam);
        device.setDeviceColor(deviceColor);
        device.setDevicePrice(devicePrice);
        device.setDeviceStock(deviceStock);
        device.setDeviceCondition(deviceCondition);
    }
}
