package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Asignacion;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.AsignacionRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vínculo formal paciente–profesional, llamado también "asignaciones".
 *
 * Crear / liberar: ADMIN, DIRECTOR y JEFE_* (el jefe solo dentro de su propio
 * departamento). Consultas: el personal clínico ve SUS asignaciones activas
 * (/{mias}), el jefe ve las de su equipo, y cualquier clínico puede consultar
 * las asignaciones activas de un paciente para el expediente.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    /** Roles clínicos que pueden tener pacientes asignados. */
    static final List<String> ROLES_PERSONAL = List.of("DOCTOR", "PSICOLOGO", "PSIQUIATRA", "ENFERMERO", "TRABAJADOR_SOCIAL");

    static final String PERMISO_GESTION =
        "hasRole('ADMIN') or hasRole('DIRECTOR') or hasRole('JEFE_PSICOLOGIA') or " +
        "hasRole('JEFE_PSIQUIATRIA') or hasRole('JEFE_TRABAJO_SOCIAL') or hasRole('JEFE_ENFERMERIA') or hasRole('JEFE_ARCHIVO')";

    @Autowired
    private AsignacionRepository asignacionRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    // ==================== CONSULTA: MIS PACIENTES ASIGNADOS ====================
    @GetMapping("/mias")
    public ResponseEntity<?> misAsignaciones() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario yo = usuarioRepository.findByUsername(username).orElse(null);
        if (yo == null) {
            return ResponseEntity.status(401).body("Usuario no autenticado");
        }
        List<Asignacion> lista = asignacionRepository.findByUsuarioIdAndActivaTrue(yo.getId());
        return ResponseEntity.ok(lista);
    }

    // ==================== CONSULTA: ASIGNACIONES DE UN PROFESIONAL ====================
    @GetMapping("/persona/{usuarioId}")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> asignacionesDePersona(@PathVariable Long usuarioId) {
        Usuario objetivo = usuarioRepository.findById(usuarioId).orElse(null);
        if (objetivo == null) {
            return ResponseEntity.badRequest().body("Profesional no encontrado");
        }

        ResponseEntity<?> bloqueo = validarPermisoDepto(objetivo);
        if (bloqueo != null) return bloqueo;

        return ResponseEntity.ok(asignacionRepository.findByUsuarioIdAndActivaTrue(usuarioId));
    }

    // ==================== CONSULTA: ASIGNACIONES DE UN PACIENTE ====================
    @GetMapping("/paciente/{clienteId}")
    public ResponseEntity<?> asignacionesDePaciente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(asignacionRepository.findByClienteIdAndActivaTrue(clienteId));
    }

    // ==================== CREAR ASIGNACION ====================
    @PostMapping
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByUsername(username).orElse(null);
            if (actor == null) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            Number clienteIdNum = (Number) payload.get("clienteId");
            Number usuarioIdNum = (Number) payload.get("usuarioId");
            if (clienteIdNum == null || usuarioIdNum == null) {
                return ResponseEntity.badRequest().body("clienteId y usuarioId son obligatorios");
            }

            Cliente cliente = clienteRepository.findById(clienteIdNum.longValue()).orElse(null);
            if (cliente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }

            Usuario profesional = usuarioRepository.findById(usuarioIdNum.longValue()).orElse(null);
            if (profesional == null) {
                return ResponseEntity.badRequest().body("Profesional no encontrado");
            }

            // El personal debe tener un rol clínico real
            if (!ROLES_PERSONAL.contains(profesional.getRol())) {
                return ResponseEntity.badRequest().body("Solo puedes asignar pacientes al personal clínico (DOCTOR, PSICOLOGO, PSIQUIATRA, ENFERMERO o TRABAJADOR_SOCIAL)");
            }

            // Departamentos deben coincidir: el profesional pertenece a un depto y
            // el jefe solo asigna dentro del suyo.
            String deptoProfesional = departamentoDe(profesional);
            if (deptoProfesional == null) {
                return ResponseEntity.badRequest().body("El profesional no pertenece a un departamento clínico");
            }
            if (!"ADMIN".equals(actor.getRol()) && !"DIRECTOR".equals(actor.getRol())) {
                String deptoActor = departamentoDe(actor);
                if (deptoActor == null || !deptoActor.equals(deptoProfesional)) {
                    return ResponseEntity.status(403).body("Solo puedes asignar pacientes a profesionales de tu propio departamento");
                }
            }

            // Evitar duplicados
            if (asignacionRepository.existsByUsuarioAndClienteAndActivaTrue(profesional, cliente)) {
                return ResponseEntity.badRequest().body("Ese paciente ya está asignado a este profesional");
            }

            Asignacion asignacion = new Asignacion();
            asignacion.setCliente(cliente);
            asignacion.setUsuario(profesional);
            asignacion.setDepartamento(deptoProfesional);
            asignacion.setFechaInicio(LocalDate.now());
            asignacion.setActiva(true);
            asignacion.setMotivo(payload.get("motivo") != null ? payload.get("motivo").toString() : "Asignación por jefatura");

            Asignacion nueva = asignacionRepository.save(asignacion);

            auditoriaService.registrar(actor.getUsername(), "ASIGNACION",
                "Paciente '" + cliente.getNombre() + "' asignado a " + profesional.getUsername()
                + " (" + deptoProfesional + ")");

            return ResponseEntity.ok(nueva);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al asignar: " + e.getMessage());
        }
    }

    // ==================== LIBERAR ASIGNACION (baja lógica) ====================
    @DeleteMapping("/{id}")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> liberar(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actor = usuarioRepository.findByUsername(username).orElse(null);
        Asignacion asignacion = asignacionRepository.findById(id).orElse(null);

        if (asignacion == null || !asignacion.isActiva()) {
            return ResponseEntity.badRequest().body("Asignación no encontrada o ya liberada");
        }

        ResponseEntity<?> bloqueo = validarPermisoDepto(asignacion.getUsuario());
        if (bloqueo != null) return bloqueo;

        asignacion.setActiva(false);
        asignacion.setFechaFin(LocalDate.now());
        asignacionRepository.save(asignacion);

        auditoriaService.registrar(
            actor != null ? actor.getUsername() : "sistema", "DESASIGNACION",
            "Asignación #" + id + " liberada (paciente '" + asignacion.getCliente().getNombre()
            + "' de " + asignacion.getUsuario().getUsername() + ")");

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("ok", true);
        respuesta.put("mensaje", "Asignación liberada");
        return ResponseEntity.ok(respuesta);
    }

    // ==================== UTILIDADES ====================

    /** Departamento efectivo de un usuario, derivado de su campo/departamento o rol. */
    private String departamentoDe(Usuario usuario) {
        if (usuario.getDepartamento() != null && !usuario.getDepartamento().isEmpty()) {
            return usuario.getDepartamento();
        }
        if (usuario.getRol() == null) return null;
        return switch (usuario.getRol()) {
            case "PSICOLOGO", "JEFE_PSICOLOGIA" -> "PSICOLOGIA";
            case "PSIQUIATRA", "JEFE_PSIQUIATRIA" -> "PSIQUIATRIA";
            case "ENFERMERO", "JEFE_ENFERMERIA" -> "ENFERMERIA";
            case "TRABAJADOR_SOCIAL", "JEFE_TRABAJO_SOCIAL" -> "TRABAJO_SOCIAL";
            default -> null;
        };
    }

    /**
     * Bloquea la operación si el actor no es ADMIN/DIRECTOR y el profesional
     * objetivo no es de su mismo departamento. Devuelve null si todo está bien.
     */
    private ResponseEntity<?> validarPermisoDepto(Usuario objetivo) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actor = usuarioRepository.findByUsername(username).orElse(null);
        if (actor == null) {
            return ResponseEntity.status(401).body("Usuario no autenticado");
        }
        if ("ADMIN".equals(actor.getRol()) || "DIRECTOR".equals(actor.getRol())) {
            return null;
        }
        String deptoActor = departamentoDe(actor);
        String deptoObjetivo = departamentoDe(objetivo);
        if (deptoActor == null || !deptoActor.equals(deptoObjetivo)) {
            return ResponseEntity.status(403).body("Fuera de tu departamento");
        }
        return null;
    }
}
