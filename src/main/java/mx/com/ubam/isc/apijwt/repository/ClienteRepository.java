package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import mx.com.ubam.isc.apijwt.model.Cliente;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
    Cliente findByCorreo(String correo);
    Optional<Cliente> findByCorreoIgnoreCase(String correo);

    /**
     * Pacientes sin ninguna asignación activa: nadie del personal clínico los tiene.
     * Es la bandeja que el jefe usa para repartirlos en lote (SC-05).
     */
    @Query("SELECT c FROM Cliente c WHERE NOT EXISTS (" +
           "SELECT a FROM Asignacion a WHERE a.cliente = c AND a.activa = true) ORDER BY c.nombre")
    List<Cliente> findSinAsignacionActiva();
}