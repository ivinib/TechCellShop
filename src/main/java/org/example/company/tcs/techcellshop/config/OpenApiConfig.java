package org.example.company.tcs.techcellshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI techCellShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TechCell Shop API")
                        .version("v1")
                        .description("API documentation for TechCell Shop application")
                        .contact(new Contact()
                                .name("Vinicius")
                                .email("fsedragon@gmail.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSchemas("ErrorResponse", errorResponseSchema())
                        .addExamples("ValidationErrorExample", validationErrorExample())
                        .addExamples("InvalidArgumentExample", invalidArgumentExample())
                        .addExamples("NotFoundErrorExample", notFoundErrorExample())
                        .addExamples("BusinessConflictExample", businessConflictExample())
                        .addExamples("InternalErrorExample", internalErrorExample())
                        .addExamples("UnauthorizedErrorExample", unauthorizedErrorExample())
                        .addExamples("ForbiddenErrorExample", forbiddenErrorExample()))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private Schema<?> errorResponseSchema() {
        return new ObjectSchema()
                .addProperty("timestamp", new Schema<String>().example("2026-04-02T12:00:00"))
                .addProperty("status", new Schema<Integer>().example(400))
                .addProperty("error", new Schema<String>().example("Bad Request"))
                .addProperty("code", new Schema<String>().example("VALIDATION_ERROR"))
                .addProperty("message", new Schema<String>().example("Validation failed"))
                .addProperty("path", new Schema<String>().example("/api/v1/orders"))
                .addProperty("traceId", new Schema<String>().example("f7e6f2c2-96c0-4d5f-90d8-e5f7fbf2f4d1"))
                .addProperty("validationErrors", new ObjectSchema()
                        .additionalProperties(new Schema<String>())
                        .example(Map.of("quantityOrder", "must be greater than 0")));
    }

    private Example validationErrorExample() {
        return new Example().value(Map.of(
                "timestamp", "2026-04-02T12:00:00",
                "status", 400,
                "error", "Bad Request",
                "code", "VALIDATION_ERROR",
                "message", "Validation failed",
                "path", "/api/v1/orders",
                "traceId", "f7e6f2c2-96c0-4d5f-90d8-e5f7fbf2f4d1",
                "validationErrors", Map.of("quantityOrder", "must be greater than 0")
        ));
    }

    private Example invalidArgumentExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", "2026-04-02T12:00:00");
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("code", "INVALID_ARGUMENT");
        body.put("message", "Payment amount does not match the order total");
        body.put("path", "/api/v1/payments/orders/1/confirm");
        body.put("traceId", "89dcdd37-8e85-4f53-8a9b-b10b723d5316");
        body.put("validationErrors", null);
        return new Example().value(body);
    }

    private Example notFoundErrorExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", "2026-04-02T12:00:00");
        body.put("status", 404);
        body.put("error", "Not Found");
        body.put("code", "RESOURCE_NOT_FOUND");
        body.put("message", "Order not found with id: 99");
        body.put("path", "/api/v1/orders/99");
        body.put("traceId", "f13df8ad-40ad-4c47-8bb5-1e4d5d71e18a");
        body.put("validationErrors", null);
        return new Example().value(body);
    }

    private Example businessConflictExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", "2026-04-02T12:00:00");
        body.put("status", 409);
        body.put("error", "Conflict");
        body.put("code", "BUSINESS_CONFLICT");
        body.put("message", "Refund is only allowed for order with confirmed payment");
        body.put("path", "/api/v1/payments/orders/1/refund");
        body.put("traceId", "9be47dd3-f7a4-4028-b0a5-e6f6e1fc554f");
        body.put("validationErrors", null);
        return new Example().value(body);
    }

    private Example internalErrorExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", "2026-04-02T12:00:00");
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("code", "INTERNAL_ERROR");
        body.put("message", "Unexpected internal error");
        body.put("path", "/api/v1/orders");
        body.put("traceId", "9d245db8-0948-44ec-b8f0-bb9207188586");
        body.put("validationErrors", null);
        return new Example().value(body);
    }

    private Example unauthorizedErrorExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", "2026-04-02T12:00:00");
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("code", "UNAUTHORIZED");
        body.put("message", "Authentication is required to access this resource");
        body.put("path", "/api/v1/payments/orders/1/confirm");
        body.put("traceId", null);
        body.put("validationErrors", null);
        return new Example().value(body);
    }

    private Example forbiddenErrorExample() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", "2026-04-02T12:00:00");
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("code", "FORBIDDEN");
        body.put("message", "You do not have permission to access this resource");
        body.put("path", "/api/v1/payments/orders/1/confirm");
        body.put("traceId", null);
        body.put("validationErrors", null);
        return new Example().value(body);
    }
}