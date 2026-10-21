package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros (sin contexto de Spring) del servicio de clientes.
 * Se usa Mockito "clásico" ({@code @ExtendWith(MockitoExtension.class)}) porque
 * Spring Boot 3.3.5 todavía no incorpora {@code @MockitoBean}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServiceImpl — tests unitarios con Mockito")
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setId(1L);
        region.setNombre("Sudamérica");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Linus");
        cliente.setApellido("Torvalds");
        cliente.setEmail("linus@kernel.org");
        cliente.setFoto("linus.jpg");
        cliente.setRegion(region);
    }

    @Test
    @DisplayName("findById con id existente retorna el cliente")
    void findById_idExistente_retornaCliente() {
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));

        Cliente resultado = clienteService.findById(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus");
        verify(clienteDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById con id inexistente lanza ClienteNotFoundException con el id en el mensaje")
    void findById_idInexistente_lanzaClienteNotFoundException() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");

        verify(clienteDao, times(1)).findById(999L);
    }

    @Test
    @DisplayName("findAll sin paginación retorna la lista completa del repositorio")
    void findAll_sinPageable_retornaListaDeClientes() {
        Cliente otroCliente = new Cliente();
        otroCliente.setId(2L);
        otroCliente.setNombre("Grace");
        otroCliente.setApellido("Hopper");
        otroCliente.setEmail("grace@navy.mil");
        otroCliente.setRegion(region);

        when(clienteDao.findAll()).thenReturn(List.of(cliente, otroCliente));

        List<Cliente> resultado = clienteService.findAll();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(Cliente::getNombre)
                .containsExactly("Linus", "Grace");
        verify(clienteDao, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll con Pageable retorna la misma página que entrega el repositorio")
    void findAll_conPageable_retornaLaMismaPagina() {
        Pageable pageable = PageRequest.of(0, 4);
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente), pageable, 1);

        when(clienteDao.findAll(any(Pageable.class))).thenReturn(pagina);

        Page<Cliente> resultado = clienteService.findAll(pageable);

        assertThat(resultado).isSameAs(pagina);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        verify(clienteDao, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("save con datos válidos retorna el cliente persistido")
    void save_datosValidos_retornaClientePersistido() {
        when(clienteDao.save(cliente)).thenReturn(cliente);

        Cliente resultado = clienteService.save(cliente);

        assertThat(resultado).isSameAs(cliente);
        assertThat(resultado.getEmail()).isEqualTo("linus@kernel.org");
        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    @DisplayName("save con violación de integridad lanza ClienteServiceException de duplicados")
    void save_violacionDeIntegridad_lanzaClienteServiceException() {
        when(clienteDao.save(cliente))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        assertThatThrownBy(() -> clienteService.save(cliente))
                .isInstanceOf(ClienteServiceException.class)
                .hasMessageContaining("duplicados");

        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    @DisplayName("update con id existente actualiza los campos y retorna el cliente")
    void update_idExistente_actualizaCamposDelCliente() {
        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Linus Updated");
        datosNuevos.setApellido("Torvalds");
        datosNuevos.setEmail("linus2@kernel.org");
        datosNuevos.setFoto("updated.jpg");
        datosNuevos.setRegion(region);

        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteDao.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.update(1L, datosNuevos);

        assertThat(resultado.getNombre()).isEqualTo("Linus Updated");
        assertThat(resultado.getEmail()).isEqualTo("linus2@kernel.org");
        assertThat(resultado.getFoto()).isEqualTo("updated.jpg");
        assertThat(resultado.getRegion()).isEqualTo(region);
        verify(clienteDao, times(1)).findById(1L);
        verify(clienteDao, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("update con id inexistente lanza ClienteNotFoundException y no guarda")
    void update_idInexistente_lanzaClienteNotFoundException() {
        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Fantasma");
        datosNuevos.setApellido("Inexistente");
        datosNuevos.setEmail("fantasma@nowhere.org");
        datosNuevos.setRegion(region);

        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.update(999L, datosNuevos))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");

        verify(clienteDao, times(1)).findById(999L);
        verify(clienteDao, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("delete de un id existente elimina sin lanzar excepción")
    void delete_idExistente_eliminaCliente() {
        when(clienteDao.existsById(1L)).thenReturn(true);

        assertThatCode(() -> clienteService.delete(1L))
                .doesNotThrowAnyException();

        verify(clienteDao, times(1)).existsById(1L);
        verify(clienteDao, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete de un id inexistente lanza ClienteNotFoundException y no borra")
    void delete_idInexistente_lanzaClienteNotFoundException() {
        when(clienteDao.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.delete(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");

        verify(clienteDao, times(1)).existsById(999L);
        verify(clienteDao, never()).deleteById(any());
    }

    @Test
    @DisplayName("findAllRegiones retorna las regiones del repositorio")
    void findAllRegiones_retornaListaDeRegiones() {
        when(clienteDao.findAllRegiones()).thenReturn(List.of(region));

        List<Region> resultado = clienteService.findAllRegiones();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Sudamérica");
        verify(clienteDao, times(1)).findAllRegiones();
    }
}
