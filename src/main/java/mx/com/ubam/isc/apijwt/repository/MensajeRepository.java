package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Mensaje;
import mx.com.ubam.isc.apijwt.model.Usuario;
import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByDestinatarioOrderByFechaEnvioDesc(Usuario destinatario);
    List<Mensaje> findByRemitenteAndDestinatarioOrderByFechaEnvioDesc(Usuario remitente, Usuario destinatario);
    Long countByDestinatarioAndLeidoFalse(Usuario destinatario);
}