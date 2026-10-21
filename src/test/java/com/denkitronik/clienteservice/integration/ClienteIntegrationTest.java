package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests end-to-end reales contra un PostgreSQL levantado con Testcontainers.
 * <p>
 * {@code disabledWithoutDocker = true} hace que, si Docker no está disponible,
 * los tests se marquen como <b>skipped</b> en lugar de fallar: la suite se puede
 * ejecutar en cualquier máquina y, cuando hay Docker, se ejecuta de verdad.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ClienteIntegrationTest — E2E con PostgreSQL (Testcontainers)")
class ClienteIntegrationTest {

    private static final String BASE = "/api/v1/cliente-service";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final AtomicBoolean seeded = new AtomicBoolean(false);
    private static Long regionId;
    private static Long idCreado;

    @BeforeEach
    void setUp() {
        if (seeded.compareAndSet(false, true)) {
            jdbcTemplate.execute(
                    "INSERT INTO regiones(nombre) VALUES ('America del Sur')");
            regionId = jdbcTemplate.queryForObject(
                    "SELECT id FROM regiones WHERE nombre = 'America del Sur'", Long.class);
        }
    }

    private HttpHeaders headersJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Region regionSeed() {
        Region region = new Region();
        region.setId(regionId);
        region.setNombre("America del Sur");
        return region;
    }

    @Test
    @Order(1)
    @DisplayName("POST /clientes crea el cliente y devuelve 201 con id")
    void crearCliente_datosValidos_retorna201ConId() {
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Margaret");
        nuevo.setApellido("Hamilton");
        nuevo.setEmail("margaret@apollo.nasa");
        nuevo.setFoto("margaret.jpg");
        nuevo.setRegion(regionSeed());

        ResponseEntity<Cliente> respuesta = restTemplate.postForEntity(
                BASE + "/clientes", new HttpEntity<>(nuevo, headersJson()), Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getId()).isNotNull();
        assertThat(respuesta.getBody().getEmail()).isEqualTo("margaret@apollo.nasa");

        idCreado = respuesta.getBody().getId();
    }

    @Test
    @Order(2)
    @DisplayName("GET /clientes/{id} recupera el cliente creado")
    void buscarCliente_idCreado_retorna200ConEmail() {
        ResponseEntity<Cliente> respuesta = restTemplate.getForEntity(
                BASE + "/clientes/" + idCreado, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getEmail()).isEqualTo("margaret@apollo.nasa");
    }

    @Test
    @Order(3)
    @DisplayName("GET /clientes lista los clientes e incluye al creado")
    void listarClientes_contieneClienteCreado() {
        ResponseEntity<String> respuesta = restTemplate.getForEntity(
                BASE + "/clientes", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("margaret@apollo.nasa");
    }

    @Test
    @Order(4)
    @DisplayName("GET /clientes/page/0 devuelve la estructura paginada")
    void listarClientesPaginado_devuelveEstructuraDePagina() {
        ResponseEntity<String> respuesta = restTemplate.getForEntity(
                BASE + "/clientes/page/0", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("totalElements");
        assertThat(respuesta.getBody()).contains("content");
    }

    @Test
    @Order(5)
    @DisplayName("PUT /clientes/{id} actualiza el apellido y devuelve 201")
    void actualizarCliente_datosValidos_retorna201() {
        Cliente actualizado = new Cliente();
        actualizado.setNombre("Margaret");
        actualizado.setApellido("Hamilton-Updated");
        actualizado.setEmail("margaret@apollo.nasa");
        actualizado.setFoto("margaret.jpg");
        actualizado.setRegion(regionSeed());

        ResponseEntity<Cliente> respuesta = restTemplate.exchange(
                BASE + "/clientes/" + idCreado,
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(actualizado, headersJson()),
                Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getApellido()).isEqualTo("Hamilton-Updated");
    }

    @Test
    @Order(6)
    @DisplayName("GET /clientes/9999 devuelve 404")
    void buscarCliente_idInexistente_retorna404() {
        ResponseEntity<String> respuesta = restTemplate.getForEntity(
                BASE + "/clientes/9999", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /clientes/{id} elimina el cliente y luego devuelve 404")
    void eliminarCliente_luegoBusqueda_retorna404() {
        restTemplate.delete(BASE + "/clientes/" + idCreado);

        ResponseEntity<String> respuesta = restTemplate.getForEntity(
                BASE + "/clientes/" + idCreado, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
