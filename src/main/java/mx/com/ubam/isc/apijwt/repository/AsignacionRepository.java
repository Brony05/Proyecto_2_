package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Asignacion;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Cliente;
import java.util.List;
import java.util.Optional;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    /** Asignaciones activas de un profesional (lo que ve en su panel). */
    List<Asignacion> findByUsuarioIdAndActivaTrue(Long usuarioId);

    /** Asignaciones activas de un paciente (útil para el expediente). */
    List<Asignacion> findByClienteIdAndActivaTrue(Long clienteId);

    /** Asignaciones activas de un departamento (las del equipo del jefe). */
    List<Asignacion> findByDepartamentoAndActivaTrue(String departamento);

    /** Par paciente–profesional ya asignado y con vínculo vigente. */
    Optional<Asignacion> findFirstByUsuarioIdAndClienteIdAndActivaTrue(Long usuarioId, Long clienteId);

    /** Existe algún vínculo activo entre paciente y profesional. */
    boolean existsByUsuarioIdAndClienteIdAndActivaTrue(Long usuarioId, Long clienteId);

    /** Historial completo de un paciente (activas e inactivas). */
    List<Asignacion> findByClienteIdOrderByFechaInicioDesc(Long clienteId);

    boolean existsByUsuarioAndClienteAndActivaTrue(Usuario usuario, Cliente cliente);
}
