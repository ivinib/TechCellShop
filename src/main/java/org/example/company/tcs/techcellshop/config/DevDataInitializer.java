package org.example.company.tcs.techcellshop.config;

import org.example.company.tcs.techcellshop.domain.Device;
import org.example.company.tcs.techcellshop.domain.Order;
import org.example.company.tcs.techcellshop.domain.User;
import org.example.company.tcs.techcellshop.repository.DeviceRepository;
import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    CommandLineRunner seedData(
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            OrderRepository orderRepository
    ) {
        return args -> {
            // Prevent duplicate inserts when using persistent DB
            if (userRepository.count() > 0 || deviceRepository.count() > 0 || orderRepository.count() > 0) {
                return;
            }

            User user1 = new User();
            user1.setNameUser("Ana Silva");
            user1.setEmailUser("ana@techcellshop.com");
            user1.setPasswordUser("123456");
            user1.setPhoneUser("+55 11 90000-0001");
            user1.setAddressUser("Sao Paulo - SP");
            user1.setRoleUser("CUSTOMER");

            User user2 = new User();
            user2.setNameUser("Carlos Souza");
            user2.setEmailUser("carlos@techcellshop.com");
            user2.setPasswordUser("123456");
            user2.setPhoneUser("+55 11 90000-0002");
            user2.setAddressUser("Campinas - SP");
            user2.setRoleUser("ADMIN");

            user1 = userRepository.save(user1);
            user2 = userRepository.save(user2);

            Device d1 = new Device();
            d1.setNameDevice("Galaxy S24");
            d1.setDescriptionDevice("Samsung smartphone 256GB");
            d1.setDeviceType("SMARTPHONE");
            d1.setDeviceStorage("256GB");
            d1.setDeviceRam("8GB");
            d1.setDeviceColor("Black");
            d1.setDevicePrice(3999.90);
            d1.setDeviceStock(10);
            d1.setDeviceCondition("NEW");

            Device d2 = new Device();
            d2.setNameDevice("ThinkPad X1 Carbon");
            d2.setDescriptionDevice("Lenovo laptop 1TB SSD");
            d2.setDeviceType("LAPTOP");
            d2.setDeviceStorage("1TB");
            d2.setDeviceRam("16GB");
            d2.setDeviceColor("Gray");
            d2.setDevicePrice(8999.00);
            d2.setDeviceStock(5);
            d2.setDeviceCondition("REFURBISHED");

            d1 = deviceRepository.save(d1);
            d2 = deviceRepository.save(d2);

            Order o1 = new Order();
            o1.setUser(user1);
            o1.setDevice(d1);
            o1.setQuantityOrder(1);
            o1.setTotalPriceOrder(d1.getDevicePrice());
            o1.setStatusOrder("CREATED");
            o1.setOrderDate("2026-03-23");
            o1.setDeliveryDate("2026-03-27");
            o1.setPaymentMethod("PIX");
            o1.setPaymentStatus("PAID");

            Order o2 = new Order();
            o2.setUser(user2);
            o2.setDevice(d2);
            o2.setQuantityOrder(1);
            o2.setTotalPriceOrder(d2.getDevicePrice());
            o2.setStatusOrder("PROCESSING");
            o2.setOrderDate("2026-03-23");
            o2.setDeliveryDate("2026-03-29");
            o2.setPaymentMethod("CREDIT_CARD");
            o2.setPaymentStatus("AUTHORIZED");

            orderRepository.save(o1);
            orderRepository.save(o2);
        };
    }
}

