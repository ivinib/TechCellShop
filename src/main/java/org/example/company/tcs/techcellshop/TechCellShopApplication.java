package org.example.company.tcs.techcellshop;

import org.example.company.tcs.techcellshop.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class TechCellShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechCellShopApplication.class, args);
    }

}
