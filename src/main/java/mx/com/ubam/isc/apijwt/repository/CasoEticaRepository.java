package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.CasoEtica;
import java.util.List;

public interface CasoEticaRepository extends JpaRepository<CasoEtica, Long> {
    List<CasoEtica> findAllByOrderByFechaDesc();
    List<CasoEtica> findByEstado(String estado);
    List<CasoEtica> findByPacienteId(Long pacienteId);
}
