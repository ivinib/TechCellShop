package org.example.company.tcs.techcellshop;

import org.example.company.tcs.techcellshop.integration.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
class TechCellShopApplicationTests extends AbstractPostgresIT {

    @Test
    void contextLoads() {
    }

}
