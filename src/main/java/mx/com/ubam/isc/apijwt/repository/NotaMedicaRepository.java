package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.NotaMedica;
import mx.com.ubam.isc.apijwt.model.Cliente;
import java.util.List;

public interface NotaMedicaRepository extends JpaRepository<NotaMedica, Long> {
    List<NotaMedica> findByPacienteId(Long pacienteId);
    List<NotaMedica> findByPacienteAndTipo(Cliente paciente, String tipo);
    List<NotaMedica> findByCitaId(Long citaId);
}