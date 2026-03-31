package org.example.company.tcs.techcellshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.example.company.tcs.techcellshop.util.AppConstants.SECURITY_SCHEME_NAME;

@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI techCellShopOpenAPI(){
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
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
