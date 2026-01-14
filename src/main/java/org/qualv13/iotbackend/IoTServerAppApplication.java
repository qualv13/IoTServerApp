package org.qualv13.iotbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IoTServerAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(IoTServerAppApplication.class, args);
    }

}
