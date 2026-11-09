package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mx.com.ubam.isc.apijwt.model.Derivacion;

import java.util.List;

@Repository
public interface DerivacionRepository extends JpaRepository<Derivacion, Long> {

    /** Solicitudes creadas por un profesional (su historial). */
    List<Derivacion> findByUsuarioSolicitaIdOrderByFechaSolicitudDesc(Long usuarioId);

    /** Bandeja del jefe: solicitudes dirigidas a su departamento. */
    List<Derivacion> findByDepartamentoDestinoOrderByFechaSolicitudDesc(String departamentoDestino);

    /** Solo las pendientes de un departamento (lo que falta resolver). */
    List<Derivacion> findByDepartamentoDestinoAndEstadoOrderByFechaSolicitudAsc(String departamentoDestino, String estado);

    /** Evita solicitudes duplicadas del mismo paciente al mismo departamento. */
    boolean existsByClienteIdAndDepartamentoDestinoAndEstado(Long clienteId, String departamentoDestino, String estado);

    /** Historial de derivaciones de un paciente (expediente). */
    List<Derivacion> findByClienteIdOrderByFechaSolicitudDesc(Long clienteId);
}
