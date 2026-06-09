package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Servicio;
import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    Optional<Servicio> findByNombre(String nombre);
    List<Servicio> findByActivoTrue();
    List<Servicio> findByActivoTrueOrderByNombreAsc();
}