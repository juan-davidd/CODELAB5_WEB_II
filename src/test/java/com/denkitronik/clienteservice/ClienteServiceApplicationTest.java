package com.denkitronik.clienteservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ClienteServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (var springApplication = mockStatic(SpringApplication.class)) {
            ClienteServiceApplication.main(args);

            springApplication.verify(
                    () -> SpringApplication.run(ClienteServiceApplication.class, args)
            );
        }
    }
}
