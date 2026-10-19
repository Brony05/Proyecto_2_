package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.RegistroAuditoria;
import java.time.LocalDateTime;
import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    List<RegistroAuditoria> findAllByOrderByFechaDesc();
    List<RegistroAuditoria> findTop50ByOrderByFechaDesc();
    List<RegistroAuditoria> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);
    List<RegistroAuditoria> findByAccionOrderByFechaDesc(String accion);
    List<RegistroAuditoria> findByFechaBetweenAndAccionOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin, String accion);
    long countByAccion(String accion);
}
