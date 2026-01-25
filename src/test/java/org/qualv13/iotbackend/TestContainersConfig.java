//package org.qualv13.iotbackend;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.testcontainers.containers.PostgreSQLContainer;
//
//@TestConfiguration(proxyBeanMethods = false)
//public class TestContainersConfig {
//
//    @Bean
//    @ServiceConnection
//    public PostgreSQLContainer<?> postgreSQLContainer() {
//        // Uruchamiamy lekką wersję Postgresa (Alpine) w Dockerze
//        return new PostgreSQLContainer<>("postgres:15-alpine");
//    }
//}