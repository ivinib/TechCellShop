package org.example.company.tcs.techcellshop;

import org.example.company.tcs.techcellshop.config.OutboxProperties;
import org.example.company.tcs.techcellshop.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, OutboxProperties.class})
@EnableAsync
@EnableRetry
@EnableScheduling
public class TechCellShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechCellShopApplication.class, args);
    }

}
