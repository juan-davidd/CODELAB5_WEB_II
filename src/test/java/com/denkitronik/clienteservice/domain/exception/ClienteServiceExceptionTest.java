package com.denkitronik.clienteservice.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteServiceExceptionTest {

    @Test
    void constructorPreservesMessage() {
        ClienteServiceException exception = new ClienteServiceException("falló");

        assertThat(exception).hasMessage("falló");
        assertThat(exception.getCause()).isNull();
    }
}
