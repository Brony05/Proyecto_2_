package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Cliente;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
    Cliente findByCorreo(String correo);
    Optional<Cliente> findByCorreoIgnoreCase(String correo);
}