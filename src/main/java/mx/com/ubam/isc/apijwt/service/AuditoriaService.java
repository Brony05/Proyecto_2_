package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import mx.com.ubam.isc.apijwt.model.RegistroAuditoria;
import mx.com.ubam.isc.apijwt.repository.RegistroAuditoriaRepository;
import java.time.LocalDateTime;

/**
 * Servicio central de bitácora (pista de auditoría).
 * Registra quién hizo qué y cuándo, tal como exige un expediente clínico institucional.
 * Nunca lanza excepciones: un fallo de auditoría no debe interrumpir la operación clínica.
 */
@Service
public class AuditoriaService {

    @Autowired
    private RegistroAuditoriaRepository auditoriaRepository;

    public void registrar(String usuario, String accion, String detalle) {
        try {
            RegistroAuditoria registro = new RegistroAuditoria();
            registro.setUsuario(usuario != null ? usuario : "SISTEMA");
            registro.setAccion(accion);
            registro.setDetalle(detalle != null && detalle.length() > 500 ? detalle.substring(0, 500) : detalle);
            registro.setFecha(LocalDateTime.now());
            auditoriaRepository.save(registro);
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo registrar en auditoría [" + accion + "]: " + e.getMessage());
        }
    }

    /** Helper que obtiene el username autenticado actual (o SISTEMA si no hay contexto). */
    public static String usuarioActual() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.getName() != null) ? auth.getName() : "SISTEMA";
        } catch (Exception e) {
            return "SISTEMA";
        }
    }
}
