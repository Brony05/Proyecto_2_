package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.CasoEtica;
import mx.com.ubam.isc.apijwt.model.Queja;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.repository.CasoEticaRepository;
import mx.com.ubam.isc.apijwt.repository.QuejaRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión real del Comité de Ética: casos de ética clínica (revisión y dictamen)
 * y quejas de pacientes (seguimiento y resolución). Todo persistido en base de datos.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/etica")
public class EticaController {

    private static final String ROLES_ETICA =
        "hasRole('COMITE_ETICA') or hasRole('ADMIN') or hasRole('DIRECTOR')";

    @Autowired
    private CasoEticaRepository casoRepository;

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    // ==================== HELPERS DTO ====================

    private Map<String, Object> casoDto(CasoEtica c) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", c.getId());
        dto.put("pacienteId", c.getPaciente() != null ? c.getPaciente().getId() : null);
        dto.put("pacienteNombre", c.getPaciente() != null ? c.getPaciente().getNombre() : null);
        dto.put("tipo", c.getTipo());
        dto.put("descripcion", c.getDescripcion());
        dto.put("fecha", c.getFecha());
        dto.put("estado", c.getEstado());
        dto.put("dictamen", c.getDictamen());
        dto.put("fechaRevision", c.getFechaRevision());
        dto.put("revisadoPor", c.getRevisadoPor());
        return dto;
    }

    private Map<String, Object> quejaDto(Queja q) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", q.getId());
        dto.put("pacienteId", q.getPaciente() != null ? q.getPaciente().getId() : null);
        dto.put("pacienteNombre", q.getPaciente() != null ? q.getPaciente().getNombre() : null);
        dto.put("asunto", q.getAsunto());
        dto.put("descripcion", q.getDescripcion());
        dto.put("fecha", q.getFecha());
        dto.put("estado", q.getEstado());
        dto.put("resolucion", q.getResolucion());
        dto.put("fechaResolucion", q.getFechaResolucion());
        return dto;
    }

    // ==================== CASOS DE ÉTICA ====================

    @GetMapping("/casos")
    @PreAuthorize(ROLES_ETICA)
    public ResponseEntity<?> listarCasos() {
        try {
            List<Map<String, Object>> casos = casoRepository.findAllByOrderByFechaDesc()
                    .stream().map(this::casoDto).collect(Collectors.toList());
            return ResponseEntity.ok(casos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar casos: " + e.getMessage());
        }
    }

    @PostMapping("/casos")
    @PreAuthorize(ROLES_ETICA)
    public ResponseEntity<?> crearCaso(@RequestBody Map<String, Object> payload) {
        try {
            Long pacienteId = payload.get("pacienteId") != null
                    ? ((Number) payload.get("pacienteId")).longValue() : null;
            String tipo = (String) payload.get("tipo");
            String descripcion = (String) payload.get("descripcion");

            if (pacienteId == null || tipo == null || tipo.isEmpty()) {
                return ResponseEntity.badRequest().body("El paciente y el tipo de caso son obligatorios");
            }

            Cliente paciente = clienteRepository.findById(pacienteId).orElse(null);
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }

            CasoEtica caso = new CasoEtica();
            caso.setPaciente(paciente);
            caso.setTipo(tipo);
            caso.setDescripcion(descripcion);
            caso.setFecha(LocalDate.now());
            caso.setEstado("PENDIENTE");
            casoRepository.save(caso);

            auditoriaService.registrar(AuditoriaService.usuarioActual(), "ETICA",
                    "Registro de caso de ética ID " + caso.getId() + " (" + tipo + ") del paciente '" 
                    + paciente.getNombre() + "'");

            return ResponseEntity.ok(casoDto(caso));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear caso: " + e.getMessage());
        }
    }

    /** Registra el dictamen del comité: decision = APROBADO | RECHAZADO | EN_REVISION. */
    @PutMapping("/casos/{id}/revisar")
    @PreAuthorize(ROLES_ETICA)
    public ResponseEntity<?> revisarCaso(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            CasoEtica caso = casoRepository.findById(id).orElse(null);
            if (caso == null) {
                return ResponseEntity.notFound().build();
            }

            String dictamen = (String) payload.get("dictamen");
            String decision = (String) payload.getOrDefault("decision", "APROBADO");
            if (dictamen == null || dictamen.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El dictamen es obligatorio");
            }
            if (!List.of("APROBADO", "RECHAZADO", "EN_REVISION").contains(decision)) {
                return ResponseEntity.badRequest().body("La decisión debe ser APROBADO, RECHAZADO o EN_REVISION");
            }

            caso.setDictamen(dictamen);
            caso.setEstado(decision);
            caso.setFechaRevision(LocalDateTime.now());
            caso.setRevisadoPor(AuditoriaService.usuarioActual());
            casoRepository.save(caso);

            String paciente = caso.getPaciente() != null ? caso.getPaciente().getNombre() : "ID " + id;
            auditoriaService.registrar(AuditoriaService.usuarioActual(), "ETICA",
                    "Dictamen del caso de ética ID " + id + " (paciente '" + paciente + "'): " + decision);

            return ResponseEntity.ok(casoDto(caso));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al revisar caso: " + e.getMessage());
        }
    }

    // ==================== QUEJAS ====================

    @GetMapping("/quejas")
    @PreAuthorize(ROLES_ETICA)
    public ResponseEntity<?> listarQuejas() {
        try {
            List<Map<String, Object>> quejas = quejaRepository.findAllByOrderByFechaDesc()
                    .stream().map(this::quejaDto).collect(Collectors.toList());
            return ResponseEntity.ok(quejas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar quejas: " + e.getMessage());
        }
    }

    @PostMapping("/quejas")
    @PreAuthorize(ROLES_ETICA)
    public ResponseEntity<?> crearQueja(@RequestBody Map<String, Object> payload) {
        try {
            Long pacienteId = payload.get("pacienteId") != null
                    ? ((Number) payload.get("pacienteId")).longValue() : null;
            String asunto = (String) payload.get("asunto");
            String descripcion = (String) payload.get("descripcion");

            if (pacienteId == null || asunto == null || asunto.isEmpty()) {
                return ResponseEntity.badRequest().body("El paciente y el asunto son obligatorios");
            }

            Cliente paciente = clienteRepository.findById(pacienteId).orElse(null);
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }

            Queja queja = new Queja();
            queja.setPaciente(paciente);
            queja.setAsunto(asunto);
            queja.setDescripcion(descripcion);
            queja.setFecha(LocalDate.now());
            queja.setEstado("PENDIENTE");
            quejaRepository.save(queja);

            auditoriaService.registrar(AuditoriaService.usuarioActual(), "ETICA",
                    "Registro de queja ID " + queja.getId() + " ('" + asunto + "') del paciente '" 
                    + paciente.getNombre() + "'");

            return ResponseEntity.ok(quejaDto(queja));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear queja: " + e.getMessage());
        }
    }

    /** Atiende una queja: con resolución la marca RESUELTO; sin ella, la pasa a EN_PROCESO. */
    @PutMapping("/quejas/{id}/resolver")
    @PreAuthorize(ROLES_ETICA)
    public ResponseEntity<?> resolverQueja(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Queja queja = quejaRepository.findById(id).orElse(null);
            if (queja == null) {
                return ResponseEntity.notFound().build();
            }

            String resolucion = (String) payload.get("resolucion");
            if (resolucion != null && !resolucion.trim().isEmpty()) {
                queja.setResolucion(resolucion);
                queja.setEstado("RESUELTO");
                queja.setFechaResolucion(LocalDateTime.now());
            } else {
                queja.setEstado("EN_PROCESO");
            }
            quejaRepository.save(queja);

            String paciente = queja.getPaciente() != null ? queja.getPaciente().getNombre() : "ID " + id;
            auditoriaService.registrar(AuditoriaService.usuarioActual(), "ETICA",
                    "Atención de queja ID " + id + " (paciente '" + paciente + "'): " + queja.getEstado());

            return ResponseEntity.ok(quejaDto(queja));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al resolver queja: " + e.getMessage());
        }
    }
}
