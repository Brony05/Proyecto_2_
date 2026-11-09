package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Asignacion;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Derivacion;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.AsignacionRepository;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.DerivacionRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import mx.com.ubam.isc.apijwt.service.CitaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derivación de pacientes entre departamentos clínicos.
 *
 * - El personal clínico solicita derivar a un paciente suyo (POST).
 * - El jefe del departamento DESTINO aprueba (creando asignación y cita) o
 *   rechaza. ADMIN y DIRECTOR pueden resolver cualquier departamento.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/derivaciones")
public class DerivacionController {

    static final String ROLES_CLINICOS =
        "hasAnyRole('DOCTOR','PSICOLOGO','PSIQUIATRA','ENFERMERO','TRABAJADOR_SOCIAL')";

    static final String PERMISO_GESTION =
        "hasRole('ADMIN') or hasRole('DIRECTOR') or hasRole('JEFE_PSICOLOGIA') or " +
        "hasRole('JEFE_PSIQUIATRIA') or hasRole('JEFE_TRABAJO_SOCIAL') or hasRole('JEFE_ENFERMERIA')";

    private static final List<String> ROLES_PERSONAL =
        List.of("DOCTOR", "PSICOLOGO", "PSIQUIATRA", "ENFERMERO", "TRABAJADOR_SOCIAL");

    private static final List<String> DEPARTAMENTOS =
        List.of("PSICOLOGIA", "PSIQUIATRIA", "ENFERMERIA", "TRABAJO_SOCIAL");

    @Autowired private DerivacionRepository derivacionRepository;
    @Autowired private AsignacionRepository asignacionRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private CitaService citaService;
    @Autowired private AuditoriaService auditoriaService;

    // ==================== CREAR SOLICITUD ====================
    @PostMapping
    @PreAuthorize(ROLES_CLINICOS)
    public ResponseEntity<?> solicitar(@RequestBody Map<String, Object> payload) {
        try {
            Usuario actor = usuarioActual();
            if (actor == null) return ResponseEntity.status(401).body("Usuario no autenticado");

            Number clienteIdNum = (Number) payload.get("clienteId");
            if (clienteIdNum == null) return ResponseEntity.badRequest().body("clienteId es obligatorio");
            Long clienteId = clienteIdNum.longValue();

            Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
            if (cliente == null) return ResponseEntity.badRequest().body("Paciente no encontrado");

            String destino = texto(payload.get("departamentoDestino")).toUpperCase();
            if (!DEPARTAMENTOS.contains(destino)) {
                return ResponseEntity.badRequest().body(
                    "Departamento destino inválido. Opciones: " + String.join(", ", DEPARTAMENTOS));
            }

            String motivo = texto(payload.get("motivo"));
            if (motivo.isBlank()) {
                return ResponseEntity.badRequest().body("Explica el motivo de la derivación");
            }

            String origen = departamentoDe(actor);
            if (origen == null) {
                return ResponseEntity.badRequest().body("No se pudo determinar tu departamento");
            }
            if (origen.equals(destino)) {
                return ResponseEntity.badRequest().body("El paciente ya está en tu departamento");
            }

            // Solo se deriva a un paciente propio: asignación activa o cita previa.
            boolean esPropio = asignacionRepository.existsByUsuarioIdAndClienteIdAndActivaTrue(actor.getId(), clienteId)
                    || citaRepository.findByDoctorId(actor.getId()).stream()
                        .anyMatch(c -> c.getCliente() != null && clienteId.equals(c.getCliente().getId()));
            if (!esPropio) {
                return ResponseEntity.status(403)
                    .body("Solo puedes derivar pacientes que tengas asignados.");
            }

            if (derivacionRepository.existsByClienteIdAndDepartamentoDestinoAndEstado(
                    clienteId, destino, Derivacion.PENDIENTE)) {
                return ResponseEntity.badRequest()
                    .body("Ya existe una solicitud pendiente de este paciente para " + destino);
            }

            Derivacion derivacion = new Derivacion();
            derivacion.setCliente(cliente);
            derivacion.setUsuarioSolicita(actor);
            derivacion.setDepartamentoOrigen(origen);
            derivacion.setDepartamentoDestino(destino);
            derivacion.setMotivo(motivo);
            derivacion.setEstado(Derivacion.PENDIENTE);
            derivacion.setFechaSolicitud(LocalDateTime.now());

            Derivacion guardada = derivacionRepository.save(derivacion);

            auditoriaService.registrar(actor.getUsername(), "DERIVACION_SOLICITADA",
                "Derivación del paciente '" + cliente.getNombre() + "' de " + origen + " a " + destino);

            return ResponseEntity.ok(guardada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al solicitar la derivación: " + e.getMessage());
        }
    }

    // ==================== MIS SOLICITUDES (PROFESIONAL) ====================
    @GetMapping("/mias")
    public ResponseEntity<?> misSolicitudes() {
        Usuario actor = usuarioActual();
        if (actor == null) return ResponseEntity.status(401).body("Usuario no autenticado");
        return ResponseEntity.ok(derivacionRepository.findByUsuarioSolicitaIdOrderByFechaSolicitudDesc(actor.getId()));
    }

    // ==================== BANDEJA DEL JEFE ====================
    @GetMapping("/equipo")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> bandeja() {
        Usuario actor = usuarioActual();
        if (actor == null) return ResponseEntity.status(401).body("Usuario no autenticado");

        if ("ADMIN".equals(actor.getRol()) || "DIRECTOR".equals(actor.getRol())) {
            return ResponseEntity.ok(derivacionRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaSolicitud")));
        }
        String depto = departamentoDe(actor);
        if (depto == null) return ResponseEntity.status(403).body("No perteneces a un departamento clínico");
        return ResponseEntity.ok(derivacionRepository.findByDepartamentoDestinoOrderByFechaSolicitudDesc(depto));
    }

    // ==================== HISTORIAL DE UN PACIENTE ====================
    @GetMapping("/paciente/{clienteId}")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> porPaciente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(derivacionRepository.findByClienteIdOrderByFechaSolicitudDesc(clienteId));
    }

    // ==================== APROBAR ====================
    @PostMapping("/{id}/aprobar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> aprobar(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Usuario actor = usuarioActual();
            if (actor == null) return ResponseEntity.status(401).body("Usuario no autenticado");

            Derivacion derivacion = derivacionRepository.findById(id).orElse(null);
            if (derivacion == null) return ResponseEntity.badRequest().body("Solicitud no encontrada");
            if (!Derivacion.PENDIENTE.equals(derivacion.getEstado())) {
                return ResponseEntity.badRequest().body("Esta solicitud ya fue resuelta (" + derivacion.getEstado() + ")");
            }
            if (!puedeResolver(actor, derivacion.getDepartamentoDestino())) {
                return ResponseEntity.status(403).body("Solo el jefe del departamento destino puede aprobarla");
            }

            Number usuarioIdNum = (Number) payload.get("usuarioId");
            if (usuarioIdNum == null) {
                return ResponseEntity.badRequest().body("Selecciona al profesional de destino");
            }
            Usuario profesional = usuarioRepository.findById(usuarioIdNum.longValue()).orElse(null);
            if (profesional == null) return ResponseEntity.badRequest().body("Profesional no encontrado");
            if (!ROLES_PERSONAL.contains(profesional.getRol())) {
                return ResponseEntity.badRequest().body("El destino debe ser personal clínico");
            }
            String deptoProfesional = departamentoDe(profesional);
            if (deptoProfesional == null || !deptoProfesional.equals(derivacion.getDepartamentoDestino())) {
                return ResponseEntity.badRequest()
                    .body("El profesional no pertenece a " + derivacion.getDepartamentoDestino());
            }

            Cliente cliente = derivacion.getCliente();

            // 1) Asignación destino (idempotente: si ya existe activa se reutiliza)
            Asignacion asignacion = asignacionRepository
                .findFirstByUsuarioIdAndClienteIdAndActivaTrue(profesional.getId(), cliente.getId())
                .orElse(null);
            if (asignacion == null) {
                asignacion = new Asignacion();
                asignacion.setCliente(cliente);
                asignacion.setUsuario(profesional);
                asignacion.setDepartamento(derivacion.getDepartamentoDestino());
                asignacion.setFechaInicio(LocalDate.now());
                asignacion.setActiva(true);
                asignacion.setMotivo("Derivación desde " + derivacion.getDepartamentoOrigen()
                    + (derivacion.getMotivo() != null ? ": " + derivacion.getMotivo() : ""));
                asignacion = asignacionRepository.save(asignacion);
            }

            // 2) Cita (opcional): si el jefe elige fecha y hora. Un fallo de agenda
            //    no debe tumbar la asignación ya creada, se reporta como advertencia.
            String advertencia = null;
            LocalDate fecha = parseFecha(payload.get("fecha"));
            LocalTime hora = parseHora(payload.get("hora"));
            if (fecha != null && hora != null) {
                Cita cita = new Cita();
                cita.setCliente(cliente);
                cita.setDoctor(profesional);
                cita.setUsuario(profesional);
                cita.setFecha(fecha);
                cita.setHora(hora);
                cita.setServicio(servicioDe(derivacion.getDepartamentoDestino(), texto(payload.get("servicio"))));
                cita.setEstado("AGENDADA");
                cita.setAsistencia("PENDIENTE");
                Number precioNum = (Number) payload.get("precio");
                if (precioNum != null) cita.setPrecio(precioNum.doubleValue());
                try {
                    Cita creada = citaService.crear(cita);
                    derivacion.setCitaCreadaId(creada.getId());
                    derivacion.setCitaFecha(creada.getFecha());
                    derivacion.setCitaHora(creada.getHora());
                } catch (Exception e) {
                    advertencia = "La asignación se creó, pero la cita no: " + e.getMessage();
                }
            }

            // 3) Transferencia: se libera la asignación del solicitante (si se pidió)
            boolean liberarOrigen = payload.get("liberarOrigen") == null
                || Boolean.parseBoolean(payload.get("liberarOrigen").toString());
            if (liberarOrigen) {
                for (Asignacion origen : asignacionRepository.findByUsuarioIdAndActivaTrue(derivacion.getUsuarioSolicita().getId())) {
                    if (origen.getCliente() != null && cliente.getId().equals(origen.getCliente().getId())) {
                        origen.setActiva(false);
                        origen.setFechaFin(LocalDate.now());
                        asignacionRepository.save(origen);
                    }
                }
            }

            derivacion.setAsignacionCreadaId(asignacion.getId());
            derivacion.setEstado(Derivacion.APROBADA);
            derivacion.setFechaResolucion(LocalDateTime.now());
            derivacion.setUsuarioResuelve(actor);
            derivacion.setComentarioResolucion(
                payload.get("comentario") != null ? texto(payload.get("comentario"))
                    : "Aprobada y asignada a " + profesional.getUsername());
            derivacionRepository.save(derivacion);

            auditoriaService.registrar(actor.getUsername(), "DERIVACION_APROBADA",
                "Paciente '" + cliente.getNombre() + "' derivado de " + derivacion.getDepartamentoOrigen()
                + " a " + derivacion.getDepartamentoDestino() + " (" + profesional.getUsername() + ")");

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("derivacion", derivacion);
            respuesta.put("asignacionId", asignacion.getId());
            if (advertencia != null) respuesta.put("advertencia", advertencia);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al aprobar la derivación: " + e.getMessage());
        }
    }

    // ==================== RECHAZAR ====================
    @PostMapping("/{id}/rechazar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<?> rechazar(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Usuario actor = usuarioActual();
        if (actor == null) return ResponseEntity.status(401).body("Usuario no autenticado");

        Derivacion derivacion = derivacionRepository.findById(id).orElse(null);
        if (derivacion == null) return ResponseEntity.badRequest().body("Solicitud no encontrada");
        if (!Derivacion.PENDIENTE.equals(derivacion.getEstado())) {
            return ResponseEntity.badRequest().body("Esta solicitud ya fue resuelta");
        }
        if (!puedeResolver(actor, derivacion.getDepartamentoDestino())) {
            return ResponseEntity.status(403).body("Solo el jefe del departamento destino puede rechazarla");
        }

        derivacion.setEstado(Derivacion.RECHAZADA);
        derivacion.setFechaResolucion(LocalDateTime.now());
        derivacion.setUsuarioResuelve(actor);
        derivacion.setComentarioResolucion(
            payload.get("comentario") != null ? texto(payload.get("comentario")) : "Rechazada por la jefatura");
        derivacionRepository.save(derivacion);

        auditoriaService.registrar(actor.getUsername(), "DERIVACION_RECHAZADA",
            "Derivación del paciente '" + derivacion.getCliente().getNombre() + "' a "
            + derivacion.getDepartamentoDestino() + " rechazada");

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("derivacion", derivacion);
        return ResponseEntity.ok(respuesta);
    }

    // ==================== UTILIDADES ====================

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username).orElse(null);
    }

    /** Departamento efectivo: campo departamento o deducido del rol. */
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

    /** ADMIN/DIRECTOR resuelven cualquier depto; el jefe solo el suyo. */
    private boolean puedeResolver(Usuario actor, String departamentoDestino) {
        if ("ADMIN".equals(actor.getRol()) || "DIRECTOR".equals(actor.getRol())) return true;
        String depto = departamentoDe(actor);
        return depto != null && depto.equals(departamentoDestino);
    }

    private String servicioDe(String departamento, String solicitado) {
        if (solicitado != null && !solicitado.isBlank()) return solicitado;
        return switch (departamento) {
            case "PSICOLOGIA" -> "Terapia de Seguimiento";
            case "PSIQUIATRIA" -> "Consulta de Seguimiento Psiquiátrico";
            default -> "Consulta General";
        };
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private LocalDate parseFecha(Object valor) {
        try {
            String s = texto(valor);
            return s.isEmpty() ? null : LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseHora(Object valor) {
        try {
            String s = texto(valor);
            if (s.isEmpty()) return null;
            return LocalTime.parse(s.length() == 5 ? s : s.substring(0, 5));
        } catch (Exception e) {
            return null;
        }
    }
}
