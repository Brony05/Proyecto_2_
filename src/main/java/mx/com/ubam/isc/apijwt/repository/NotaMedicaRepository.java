package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.NotaMedica;
import mx.com.ubam.isc.apijwt.model.Cliente;
import java.util.List;

public interface NotaMedicaRepository extends JpaRepository<NotaMedica, Long> {
    List<NotaMedica> findByPacienteId(Long pacienteId);
    List<NotaMedica> findByPacienteAndTipo(Cliente paciente, String tipo);

    /**
     * Versión con orden definido: la nota más reciente (por fecha, y a igualdad
     * por el id mayor) queda SIEMPRE primero. El bot de IA y el panel del paciente
     * toman el elemento [0], así que deben leer el diagnosis vigente y no "el primero"
     * que devuelva la BD sin orden garantizado.
     */
    List<NotaMedica> findByPacienteAndTipoOrderByFechaCreacionDescIdDesc(Cliente paciente, String tipo);

    List<NotaMedica> findByPacienteIdOrderByFechaCreacionDescIdDesc(Long pacienteId);
    List<NotaMedica> findByCitaId(Long citaId);
}