package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Expediente;
import java.util.List;
import java.util.Optional;

public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {
    Optional<Expediente> findByPacienteId(Long pacienteId);
    List<Expediente> findByEstado(String estado);
    List<Expediente> findAllByOrderByFechaActualizacionDesc();
}
