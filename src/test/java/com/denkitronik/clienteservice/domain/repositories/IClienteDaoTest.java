package com.denkitronik.clienteservice.domain.repositories;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del repositorio con {@code @DataJpaTest}: usa una base H2 embebida
 * y hace rollback automático al final de cada test.
 */
@DataJpaTest
@DisplayName("IClienteDao — tests de repositorio con H2 y @DataJpaTest")
class IClienteDaoTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IClienteDao clienteDao;

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setNombre("Asia");
        em.persist(region);

        cliente = new Cliente();
        cliente.setNombre("Grace");
        cliente.setApellido("Hopper");
        cliente.setEmail("grace@navy.mil");
        cliente.setFoto("grace.jpg");
        cliente.setRegion(region);
        em.persist(cliente);

        em.flush();
    }

    @Test
    @DisplayName("findById retorna el cliente persistido con su email")
    void findById_clientePersistido_retornaCliente() {
        Optional<Cliente> resultado = clienteDao.findById(cliente.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEmail()).isEqualTo("grace@navy.mil");
    }

    @Test
    @DisplayName("findAll retorna sólo el cliente creado en el setUp")
    void findAll_trasSetUp_retornaUnCliente() {
        List<Cliente> resultado = clienteDao.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Grace");
    }

    @Test
    @DisplayName("save de un segundo cliente incrementa el conteo a 2")
    void save_segundoCliente_incrementaConteo() {
        Cliente segundo = new Cliente();
        segundo.setNombre("Margaret");
        segundo.setApellido("Hamilton");
        segundo.setEmail("margaret@apollo.nasa");
        segundo.setFoto("margaret.jpg");
        segundo.setRegion(region);

        clienteDao.save(segundo);
        em.flush();

        assertThat(clienteDao.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllRegiones ejecuta la query personalizada y retorna las regiones")
    void findAllRegiones_retornaRegionesPersistidas() {
        List<Region> regiones = clienteDao.findAllRegiones();

        assertThat(regiones).hasSize(1);
        assertThat(regiones.get(0).getNombre()).isEqualTo("Asia");
    }

    @Test
    @DisplayName("deleteById elimina el cliente de la base de datos")
    void deleteById_clienteExistente_eliminaCliente() {
        Long id = cliente.getId();

        clienteDao.deleteById(id);
        em.flush();

        // em.find consulta el contexto de persistencia y, tras el flush, la BD:
        // es más fiable que clienteDao.findById porque no reutiliza una entidad
        // que pudiera quedar cacheada en la caché de primer nivel de Hibernate
        // (en un slice test la sesión sigue abierta tras el delete).
        assertThat(em.find(Cliente.class, id)).isNull();
    }

    @Test
    @DisplayName("existsById es true para el cliente existente y false para un id inexistente")
    void existsById_clienteExistenteYNoExistente() {
        assertThat(clienteDao.existsById(cliente.getId())).isTrue();
        assertThat(clienteDao.existsById(999L)).isFalse();
    }
}
