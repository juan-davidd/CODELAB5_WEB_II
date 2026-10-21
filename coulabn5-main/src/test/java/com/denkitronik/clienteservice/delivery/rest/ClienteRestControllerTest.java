package com.denkitronik.clienteservice.delivery.rest;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.services.IClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa web (slice) con MockMvc.
 * En Spring Boot 3.3.5 se usa {@code @MockBean} (no {@code @MockitoBean})
 * y Jackson 2 ({@code com.fasterxml.jackson.databind.ObjectMapper}).
 */
@WebMvcTest(ClienteRestController.class)
@DisplayName("ClienteRestController — tests de capa web con MockMvc")
class ClienteRestControllerTest {

    private static final String BASE = "/api/v1/cliente-service";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IClienteService clienteService;

    @Autowired
    private ObjectMapper objectMapper;

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(4L);
        region.setNombre("Europa");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ada");
        cliente.setApellido("Lovelace");
        cliente.setEmail("ada@babbage.uk");
        cliente.setFoto("ada.jpg");
        cliente.setRegion(region);
    }

    @Test
    @DisplayName("GET /clientes/{id} existente retorna 200 y el cliente")
    void buscarCliente_idExistente_retorna200ConCliente() throws Exception {
        when(clienteService.findById(1L)).thenReturn(cliente);

        mockMvc.perform(get(BASE + "/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@babbage.uk"));
    }

    @Test
    @DisplayName("GET /clientes/{id} inexistente retorna 404 con la clave 'mensaje' conteniendo el id")
    void buscarCliente_idInexistente_retorna404ConMensaje() throws Exception {
        when(clienteService.findById(999L)).thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(get(BASE + "/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", containsString("999")))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /clientes retorna 200 y la lista de clientes")
    void listarClientes_retorna200ConLista() throws Exception {
        Cliente otro = new Cliente();
        otro.setId(2L);
        otro.setNombre("Grace");
        otro.setApellido("Hopper");
        otro.setEmail("grace@navy.mil");
        otro.setRegion(region);

        when(clienteService.findAll()).thenReturn(List.of(cliente, otro));

        mockMvc.perform(get(BASE + "/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Ada"));
    }

    @Test
    @DisplayName("POST /clientes con datos válidos retorna 201 y el cliente creado")
    void crearCliente_datosValidos_retorna201() throws Exception {
        when(clienteService.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente argumento = invocation.getArgument(0);
            argumento.setId(1L);
            return argumento;
        });

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ada"));
    }

    @Test
    @DisplayName("POST /clientes con nombre vacío retorna 400 con errores de validación del campo 'nombre'")
    void crearCliente_nombreVacio_retorna400() throws Exception {
        Cliente invalido = new Cliente();
        invalido.setNombre("");
        invalido.setApellido("Lovelace");
        invalido.setEmail("ada@babbage.uk");
        invalido.setRegion(region);

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem(containsString("nombre"))));
    }

    @Test
    @DisplayName("POST /clientes con email inválido retorna 400")
    void crearCliente_emailInvalido_retorna400() throws Exception {
        Cliente invalido = new Cliente();
        invalido.setNombre("Ada");
        invalido.setApellido("Lovelace");
        invalido.setEmail("esto-no-es-un-email");
        invalido.setRegion(region);

        mockMvc.perform(post(BASE + "/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("PUT /clientes/{id} existente retorna 201 con el cliente actualizado")
    void actualizarCliente_idExistente_retorna201() throws Exception {
        when(clienteService.update(eq(1L), any(Cliente.class))).thenReturn(cliente);

        mockMvc.perform(put(BASE + "/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@babbage.uk"));
    }

    @Test
    @DisplayName("PUT /clientes/{id} inexistente retorna 404 con la clave 'mensaje' conteniendo el id")
    void actualizarCliente_idInexistente_retorna404ConMensaje() throws Exception {
        when(clienteService.update(eq(999L), any(Cliente.class)))
                .thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(put(BASE + "/clientes/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cliente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", containsString("999")));
    }

    @Test
    @DisplayName("DELETE /clientes/{id} existente retorna 204 sin cuerpo")
    void eliminarCliente_idExistente_retorna204() throws Exception {
        doNothing().when(clienteService).delete(1L);

        mockMvc.perform(delete(BASE + "/clientes/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("DELETE /clientes/{id} inexistente retorna 404 con la clave 'mensaje'")
    void eliminarCliente_idInexistente_retorna404ConMensaje() throws Exception {
        doThrow(new ClienteNotFoundException(999L)).when(clienteService).delete(999L);

        mockMvc.perform(delete(BASE + "/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", containsString("999")));
    }

    @Test
    @DisplayName("GET /clientes/page/{page} retorna 200 con la página de clientes")
    void listarClientesPaginado_retorna200ConPagina() throws Exception {
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente));
        when(clienteService.findAll(any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get(BASE + "/clientes/page/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].nombre").value("Ada"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
