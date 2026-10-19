package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Queja;
import java.util.List;

public interface QuejaRepository extends JpaRepository<Queja, Long> {
    List<Queja> findAllByOrderByFechaDesc();
    List<Queja> findByEstado(String estado);
    List<Queja> findByPacienteId(Long pacienteId);
}
