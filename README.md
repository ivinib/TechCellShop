# TechCellShop API

[![Java](https://img.shields.io/badge/Java-21-blue)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen)]()
[![Build](https://img.shields.io/badge/Build-Maven-orange)]()

REST API for a tech store domain with user enrollment, device catalog, and order management.  

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Security Architecture (JWT)](#security-architecture-jwt)
- [Error Handling](#error-handling)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Authentication & API Usage](#authentication--api-usage)
- [Messaging Flow](#messaging-flow)
- [Testing](#testing)
- [Portfolio Improvements (Roadmap)](#portfolio-improvements-roadmap)

---

## Features

- User enrollment and management
- Device enrollment and catalog operations
- Order placement and updates
- JWT-based authentication for protected endpoints
- Bean Validation for request payloads
- Global exception handling with standardized error responses
- RabbitMQ integration for order-created event processing
- H2 in-memory database for local/dev profile with SQL seed

---

## Tech Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- H2 Database
- RabbitMQ (Spring AMQP)
- JWT (`jjwt`)
- Maven

---

## Project Structure

```text
src/main/java/org/example/company/tcs/techcellshop
├── config/        # Security and messaging configuration
├── controller/    # REST controllers + request/response DTOs
├── domain/        # JPA entities and domain models
├── exception/     # Global exception handling
├── mapper/        # Request/response mapping
├── messaging/     # Event publisher/listener
├── repository/    # Spring Data repositories
├── security/      # JWT filter/properties/user details service
└── service/       # Business logic

src/main/resources
├── application.properties
├── application-dev.properties
└── sql/data.sql

