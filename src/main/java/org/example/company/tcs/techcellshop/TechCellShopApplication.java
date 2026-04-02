package org.example.company.tcs.techcellshop;

import org.example.company.tcs.techcellshop.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableAsync
@EnableRetry
public class TechCellShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechCellShopApplication.class, args);
    }

}
