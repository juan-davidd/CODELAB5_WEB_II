package com.denkitronik.clienteservice.delivery.exception;

import com.denkitronik.clienteservice.delivery.rest.ClienteRestController;
import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void manejarNotFoundReturnsNotFoundResponse() {
        var response = handler.manejarNotFound(new ClienteNotFoundException(8L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMensaje()).contains("8");
    }

    @Test
    void manejarServiceExceptionHidesInternalDetails() {
        var response = handler.manejarServiceException(
                new ClienteServiceException("private details")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMensaje()).doesNotContain("private details");
    }

    @Test
    void manejarValidacionReturnsFieldErrors() throws NoSuchMethodException {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(
                List.of(new FieldError("cliente", "nombre", "es obligatorio"))
        );
        Method method = ClienteRestController.class.getMethod(
                "crearCliente", Cliente.class, BindingResult.class
        );
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(method, 0), bindingResult
        );

        var response = handler.manejarValidacion(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("nombre", "es obligatorio");
    }

    @Test
    void manejarExcepcionGenericaReturnsGenericError() {
        var response = handler.manejarExcepcionGenerica(new IllegalStateException("secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMensaje()).contains("Error inesperado");
        assertThat(response.getBody().getMensaje()).doesNotContain("secret");
    }
}
