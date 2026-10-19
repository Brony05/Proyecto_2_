package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Expediente;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.repository.ExpedienteRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.NotaMedicaRepository;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión real del archivo clínico: un expediente por paciente con estado
 * (ACTIVO / INACTIVO / ARCHIVADO), observaciones de archivo y fecha de última actualización.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/expedientes")
public class ExpedienteController {

    // Autorizados para consultar y gestionar el archivo clínico
    private static final String ROLES_ARCHIVO =
        "hasRole('JEFE_ARCHIVO') or hasRole('ADMIN') or hasRole('DIRECTOR')";

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private NotaMedicaRepository notaMedicaRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    /** Convierte un expediente a un DTO plano con los datos del paciente y conteo de notas. */
    private Map<String, Object> aDto(Expediente e) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", e.getId());
        dto.put("pacienteId", e.getPaciente() != null ? e.getPaciente().getId() : null);
        dto.put("pacienteNombre", e.getPaciente() != null ? e.getPaciente().getNombre() : null);
        dto.put("pacienteTelefono", e.getPaciente() != null ? e.getPaciente().getTelefono() : null);
        dto.put("pacienteCorreo", e.getPaciente() != null ? e.getPaciente().getCorreo() : null);
        dto.put("estado", e.getEstado());
        dto.put("observaciones", e.getObservaciones());
        dto.put("fechaActualizacion", e.getFechaActualizacion());
        dto.put("numeroNotas", e.getPaciente() != null
                ? notaMedicaRepository.findByPacienteId(e.getPaciente().getId()).size() : 0);
        return dto;
    }

    @GetMapping
    @PreAuthorize(ROLES_ARCHIVO)
    public ResponseEntity<?> listar() {
        try {
            List<Map<String, Object>> expedientes = expedienteRepository.findAllByOrderByFechaActualizacionDesc()
                    .stream().map(this::aDto).collect(Collectors.toList());
            return ResponseEntity.ok(expedientes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar expedientes: " + e.getMessage());
        }
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize(ROLES_ARCHIVO)
    public ResponseEntity<?> porPaciente(@PathVariable Long pacienteId) {
        return expedienteRepository.findByPacienteId(pacienteId)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(aDto(e)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("pacienteId", pacienteId, "estado", "SIN_EXPEDIENTE")));
    }

    /** Crea o actualiza (upsert) el expediente de un paciente. */
    @PostMapping
    @PreAuthorize(ROLES_ARCHIVO)
    public ResponseEntity<?> guardar(@RequestBody Map<String, Object> payload) {
        try {
            Long pacienteId = payload.get("pacienteId") != null
                    ? ((Number) payload.get("pacienteId")).longValue() : null;
            if (pacienteId == null) {
                return ResponseEntity.badRequest().body("El pacienteId es obligatorio");
            }

            Cliente paciente = clienteRepository.findById(pacienteId).orElse(null);
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }

            String estado = (String) payload.getOrDefault("estado", "ACTIVO");
            if (!List.of("ACTIVO", "INACTIVO", "ARCHIVADO").contains(estado)) {
                return ResponseEntity.badRequest().body("El estado debe ser ACTIVO, INACTIVO o ARCHIVADO");
            }

            String observaciones = (String) payload.get("observaciones");

            Expediente expediente = expedienteRepository.findByPacienteId(pacienteId).orElse(null);
            boolean esNuevo = (expediente == null);
            if (esNuevo) {
                expediente = new Expediente();
                expediente.setPaciente(paciente);
            }
            expediente.setEstado(estado);
            expediente.setObservaciones(observaciones);
            expediente.setFechaActualizacion(LocalDateTime.now());
            expedienteRepository.save(expediente);

            auditoriaService.registrar(AuditoriaService.usuarioActual(), "EXPEDIENTE",
                    (esNuevo ? "Creación" : "Actualización") + " de expediente del paciente '" 
                    + paciente.getNombre() + "' (ID " + pacienteId + ") → estado " + estado);

            return ResponseEntity.ok(aDto(expediente));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar expediente: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize(ROLES_ARCHIVO)
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Expediente expediente = expedienteRepository.findById(id).orElse(null);
            if (expediente == null) {
                return ResponseEntity.notFound().build();
            }
            if (payload.containsKey("estado")) {
                String estado = (String) payload.get("estado");
                if (!List.of("ACTIVO", "INACTIVO", "ARCHIVADO").contains(estado)) {
                    return ResponseEntity.badRequest().body("El estado debe ser ACTIVO, INACTIVO o ARCHIVADO");
                }
                expediente.setEstado(estado);
            }
            if (payload.containsKey("observaciones")) {
                expediente.setObservaciones((String) payload.get("observaciones"));
            }
            expediente.setFechaActualizacion(LocalDateTime.now());
            expedienteRepository.save(expediente);

            auditoriaService.registrar(AuditoriaService.usuarioActual(), "EXPEDIENTE",
                    "Modificación de expediente ID " + id + " → estado " + expediente.getEstado());

            return ResponseEntity.ok(aDto(expediente));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar expediente: " + e.getMessage());
        }
    }
}
