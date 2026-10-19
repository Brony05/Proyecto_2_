package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.RegistroAuditoria;
import mx.com.ubam.isc.apijwt.repository.RegistroAuditoriaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta de la pista de auditoría del consultorio (quién hizo qué y cuándo),
 * con filtros por fecha y tipo de acción. Solo lectura.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private static final String ROLES_AUDITORIA =
        "hasRole('JEFE_ARCHIVO') or hasRole('ADMIN') or hasRole('DIRECTOR')";

    @Autowired
    private RegistroAuditoriaRepository auditoriaRepository;

    @GetMapping
    @PreAuthorize(ROLES_AUDITORIA)
    public ResponseEntity<?> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String accion) {
        try {
            List<RegistroAuditoria> registros;
            if (fecha != null && accion != null && !accion.isEmpty()) {
                LocalDateTime inicio = fecha.atStartOfDay();
                LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
                registros = auditoriaRepository.findByFechaBetweenAndAccionOrderByFechaDesc(inicio, fin, accion);
            } else if (fecha != null) {
                LocalDateTime inicio = fecha.atStartOfDay();
                LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
                registros = auditoriaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin);
            } else if (accion != null && !accion.isEmpty()) {
                registros = auditoriaRepository.findByAccionOrderByFechaDesc(accion);
            } else {
                registros = auditoriaRepository.findTop50ByOrderByFechaDesc();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("total", registros.size());
            response.put("registros", registros);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al consultar auditoría: " + e.getMessage());
        }
    }

    /** Conteo de registros por tipo de acción (para el dashboard). */
    @GetMapping("/resumen")
    @PreAuthorize(ROLES_AUDITORIA)
    public ResponseEntity<?> resumen() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("LOGIN", auditoriaRepository.countByAccion("LOGIN"));
            response.put("CREACION", auditoriaRepository.countByAccion("CREACION"));
            response.put("MODIFICACION", auditoriaRepository.countByAccion("MODIFICACION"));
            response.put("CANCELACION", auditoriaRepository.countByAccion("CANCELACION"));
            response.put("NOTA_MEDICA", auditoriaRepository.countByAccion("NOTA_MEDICA"));
            response.put("EXPEDIENTE", auditoriaRepository.countByAccion("EXPEDIENTE"));
            response.put("PAGO", auditoriaRepository.countByAccion("PAGO"));
            response.put("ETICA", auditoriaRepository.countByAccion("ETICA"));
            response.put("USUARIO", auditoriaRepository.countByAccion("USUARIO"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
