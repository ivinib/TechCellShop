# TechCellShop API

[![Java](https://img.shields.io/badge/Java-21-blue)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen)]()
[![Build](https://img.shields.io/badge/Build-Maven-orange)]()
[![Database](https://img.shields.io/badge/Database-PostgreSQL-336791)]()
[![Messaging](https://img.shields.io/badge/Messaging-RabbitMQ-ff6600)]()
[![Docs](https://img.shields.io/badge/API-Swagger-success)]()

TechCellShop is a backend application for a tech retail domain, built with Spring Boot as a **modular monolith**.

It is designed to showcase backend engineering concerns beyond basic CRUD, including:

- JWT-based authentication and authorization
- order lifecycle and payment business rules
- idempotent order placement
- reliable event publication using the outbox pattern
- asynchronous processing with RabbitMQ
- standardized API error responses
- observability with Actuator and Micrometer
- local infrastructure with Docker, PostgreSQL, and RabbitMQ

---

## Table of Contents

- [Summary of the purpose of this project](#Summary of the purpose of this project)
- [Key features](#key-features)
- [Architecture](#architecture)
- [System architecture diagram](#system-architecture-diagram)
- [Order placement flow](#order-placement-flow)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Security](#security)
- [API documentation](#api-documentation)
- [Error handling](#error-handling)
- [Observability](#observability)
- [Outbox event recovery](#outbox-event-recovery)
- [Running locally](#running-locally)
- [Local URLs](#local-urls)
- [Testing](#testing)
- [Where to look in the code first](#where-to-look-in-the-code-first)
- [Future improvements](#future-improvements)

---

## Summary of the purpose of this project

This project intentionally goes beyond a simple CRUD API.

It demonstrates practical backend concerns that are common in real-world systems:

- **Security**
  - JWT authentication
  - stateless authorization
  - role-based access control

- **Business workflow handling**
  - order placement
  - status transitions
  - cancellations
  - coupon application
  - payment state management

- **Reliability**
  - outbox pattern for event publishing
  - retry-aware asynchronous publishing
  - duplicate-safe event consumption

- **API quality**
  - validation on incoming requests
  - centralized exception handling
  - standardized error payloads
  - OpenAPI/Swagger documentation

- **Operational readiness**
  - health endpoints
  - readiness and liveness groups
  - business and messaging metrics
  - Dockerized local stack

This project is intentionally built as a **modular monolith instead of premature microservices**, focusing first on clean boundaries, business correctness, reliability, and observability.

---

## Key features

- **User management**
  - user enrollment
  - secured access to user operations

- **Device catalog**
  - device registration and management
  - stock-aware order integration

- **Order lifecycle**
  - place new order
  - list authenticated user orders
  - get order by id
  - update order status
  - cancel order
  - apply coupon to an order

- **Payment flow**
  - confirm payment
  - fail payment
  - refund payment for eligible canceled orders

- **Coupon flow**
  - validate and apply discount logic

- **JWT security**
  - bearer token authentication
  - protected endpoints based on roles

- **Standardized error handling**
  - validation errors
  - not found errors
  - invalid argument errors
  - business conflict errors
  - unauthorized and forbidden responses

- **Messaging and reliability**
  - order-created outbox events
  - scheduled outbox publisher
  - RabbitMQ consumer with duplicate protection

- **Observability**
  - Actuator health groups
  - custom `orderFlow` health indicator
  - Micrometer counters and timers

- **Deployment-ready local setup**
  - Docker Compose
  - PostgreSQL
  - RabbitMQ
  - optional pgAdmin profile

---

## Architecture

The application is designed as a **modular monolith**.

It runs as a single deployable service, but the codebase is structured into clear modules and responsibilities:

- `controller`
  - REST API layer
  - request handling
  - response building

- `service`
  - business orchestration

- `service.impl.order`
  - extracted order use cases:
    - `PlaceOrder`
    - `UpdateOrderStatus`
    - `CancelOrder`
    - `ApplyCouponToOrder`

- `repository`
  - persistence access through Spring Data

- `domain`
  - JPA entities and business state

- `security` and `config`
  - JWT and security configuration
  - OpenAPI configuration
  - messaging configuration

- `messaging`
  - outbox publisher
  - message listener
  - asynchronous integration flow

- `mapper`
  - request/response mapping

- `exception`
  - centralized API exception handling

This structure keeps the system simple to run while still demonstrating separation of concerns and clear boundaries for future evolution.

---

## System architecture diagram

```mermaid
flowchart LR
    Client[Client / Postman / Swagger UI]

    subgraph App[TechCellShop Spring Boot Application]
        Controllers[Controllers\nAuth, User, Device, Order,\nPayment, Coupon, Outbox Admin]
        Security[JWT Security Layer\nFilter + Authorization Rules]
        Services[Business Services]
        UseCases[Order Use Cases\nPlaceOrder, CancelOrder,\nUpdateOrderStatus, ApplyCouponToOrder]
        Mappers[Mappers + DTOs]
        Errors[Global Exception Handler]
        Outbox[Outbox Publisher Job]
        Listener[OrderCreatedListener]
        Health[Actuator + Health Indicators]
    end

    DB[(PostgreSQL)]
    Rabbit[(RabbitMQ)]

    Client --> Controllers
    Controllers --> Security
    Security --> Services
    Services --> UseCases
    Controllers --> Mappers
    Controllers --> Errors

    Services --> DB
    UseCases --> DB

    UseCases --> Outbox
    Outbox --> Rabbit
    Rabbit --> Listener
    Listener --> DB

    Health --> DB
    Health --> Rabbit