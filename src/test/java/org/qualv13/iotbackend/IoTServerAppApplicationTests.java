package org.qualv13.iotbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Without the profile this loads application.yaml, which expects the R2 keys
// and a real database to be present in the environment. The point of a context
// test is that it runs on a clean checkout.
@SpringBootTest
@ActiveProfiles("test")
class IoTServerAppApplicationTests {

    @Test
    void contextLoads() {

    }

}
