package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Pago;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findAllByOrderByFechaDesc();
    Optional<Pago> findByCitaId(Long citaId);
    List<Pago> findByCitaFechaBetween(LocalDate inicio, LocalDate fin);
    boolean existsByCitaId(Long citaId);
}
