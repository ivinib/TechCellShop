package org.example.company.tcs.techcellshop.messaging;

import org.example.company.tcs.techcellshop.domain.Order;

public record OrderCreatedDomainEvent(Order order) { }
