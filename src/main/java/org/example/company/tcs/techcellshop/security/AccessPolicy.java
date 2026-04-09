package org.example.company.tcs.techcellshop.security;

import org.example.company.tcs.techcellshop.repository.OrderRepository;
import org.example.company.tcs.techcellshop.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accessPolicy")
public class AccessPolicy {
    private final UserRepository userRepository;

    private OrderRepository orderRepository;

    public AccessPolicy(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public boolean canAccessUser(Long userId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> user.getEmailUser() != null
                        && user.getEmailUser().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }

    public boolean canAccessOrder(Long orderId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return false;
        }

        return orderRepository.findById(orderId)
                .map(order -> order.getUser() != null
                        && order.getUser().getEmailUser() != null
                        && order.getUser().getEmailUser().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}
