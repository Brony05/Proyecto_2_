package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.*;
import mx.com.ubam.isc.apijwt.repository.*;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/notas")
public class NotaMedicaController {

    // Roles del personal clínico autorizados para crear y consultar notas
    // (incluye al Trabajador Social, que registra intervenciones)
    private static final String ROLES_CLINICOS =
        "hasRole('DOCTOR') or hasRole('ADMIN') or hasRole('PSICOLOGO') or " +
        "hasRole('PSIQUIATRA') or hasRole('ENFERMERO') or hasRole('TRABAJADOR_SOCIAL')";
    // Consulta de notas: el personal clínico más Archivo Clínico y Dirección (lectura)
    private static final String ROLES_LECTURA =
        ROLES_CLINICOS + " or hasRole('JEFE_ARCHIVO') or hasRole('DIRECTOR')";

    @Autowired
    private NotaMedicaRepository notaRepository;
    
    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaService auditoriaService;


 // En NotaMedicaController.java, asegurar que el método crearNota guarda correctamente

    @PostMapping
    @PreAuthorize(NotaMedicaController.ROLES_CLINICOS)
    public ResponseEntity<?> crearNota(@RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario doctor = usuarioRepository.findByUsername(username).orElse(null);
            
            if (doctor == null) {
                return ResponseEntity.status(401).body("Doctor no autenticado");
            }
            
            Long pacienteId = ((Number) payload.get("pacienteId")).longValue();
            String tipo = payload.get("tipo") != null ? payload.get("tipo").toString().trim().toUpperCase() : "";

            Cliente paciente = clienteRepository.findById(pacienteId).orElse(null);
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }

            // Asignación de pacientes: el personal clínico solo escribe notas de SUS
            // propios pacientes (un paciente es propio si tiene cita con este doctor)
            if (doctor.getRol() != null && List.of("DOCTOR", "PSICOLOGO", "PSIQUIATRA", "ENFERMERO", "TRABAJADOR_SOCIAL").contains(doctor.getRol())) {
                boolean esPropio = citaRepository.findByDoctorId(doctor.getId()).stream()
                        .anyMatch(c -> c.getCliente() != null && pacienteId.equals(c.getCliente().getId()));
                if (!esPropio) {
                    return ResponseEntity.status(403).body("Este paciente no está asignado a ti. Solo puedes registrar notas de tus propios pacientes.");
                }
            }
            
            NotaMedica nota = new NotaMedica();
            nota.setPaciente(paciente);
            nota.setDoctor(doctor);
            nota.setTipo(tipo);
            nota.setFechaCreacion(LocalDateTime.now());
            
            // Extraer TODOS los campos enviados (el bot puede generar varios a la vez)
            String diagnostico = textoDe(payload.get("diagnostico"));
            String recomendaciones = textoDe(payload.get("recomendaciones"));
            String medicamentos = textoDe(payload.get("medicamentos"));
            
            if (tipo.isBlank()) {
                tipo = "CONSEJO";
            }
            
            // FIX bug del diagnóstico: si el campo principal del tipo viene vacío,
            // se respalda con el primer contenido disponible (nunca se guarda una nota vacía)
            boolean campoPrincipalVacio =
                ("DIAGNOSTICO".equals(tipo) && diagnostico.isBlank())
                || ("RECOMENDACION".equals(tipo) && recomendaciones.isBlank())
                || (("MEDICAMENTO".equals(tipo) || "INTERVENCION".equals(tipo)) && medicamentos.isBlank())
                || ("CONSEJO".equals(tipo) && recomendaciones.isBlank());
            if (campoPrincipalVacio) {
                String respaldo = !diagnostico.isBlank() ? diagnostico
                        : !recomendaciones.isBlank() ? recomendaciones : medicamentos;
                switch (tipo) {
                    case "DIAGNOSTICO" -> diagnostico = respaldo;
                    case "RECOMENDACION", "CONSEJO" -> recomendaciones = respaldo;
                    default -> medicamentos = respaldo;
                }
            }
            
            if (diagnostico.isBlank() && recomendaciones.isBlank() && medicamentos.isBlank()) {
                return ResponseEntity.badRequest().body("El contenido de la nota está vacío");
            }
            
            // Guardar todo lo que llegó: una nota generada por el bot conserva
            // diagnóstico + recomendaciones + medicamentos juntos
            nota.setDiagnostico(diagnostico.isBlank() ? null : diagnostico);
            nota.setRecomendaciones(recomendaciones.isBlank() ? null : recomendaciones);
            nota.setMedicamentos(medicamentos.isBlank() ? null : medicamentos);
            
            notaRepository.save(nota);
            
            auditoriaService.registrar(AuditoriaService.usuarioActual(), "NOTA_MEDICA",
                "Registro de nota " + tipo + " para el paciente '" + paciente.getNombre()
                + "' (ID " + pacienteId + ")");
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Nota guardada correctamente");
            response.put("nota", nota);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear nota: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error al crear nota: " + e.getMessage());
        }
    }
    
    
    
    
    
    /**
     * Convierte un valor del payload a texto limpio (null-safe).
     */
    private String textoDe(Object valor) {
        return valor != null ? valor.toString().trim() : "";
    }

    // Paciente: Ver sus notas médicas
    @GetMapping("/paciente")
    public ResponseEntity<?> getMisNotas() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Cliente paciente = clienteRepository.findByCorreo(username);
            if (paciente == null) {
                paciente = clienteRepository.findByCorreoIgnoreCase(username).orElse(null);
            }

            // Auto-provisión: si el paciente aún no tiene ficha, crearla
            // (misma conducta que el agendado de citas) para que nunca vea un error
            if (paciente == null) {
                paciente = new Cliente();
                paciente.setNombre(username.contains("@") ? username.split("@")[0] : username);
                paciente.setCorreo(username);
                paciente.setTelefono("Por definir");
                paciente = clienteRepository.save(paciente);
            }
            
            List<NotaMedica> diagnosticos = notaRepository.findByPacienteAndTipo(paciente, "DIAGNOSTICO");
            List<NotaMedica> recomendaciones = notaRepository.findByPacienteAndTipo(paciente, "RECOMENDACION");
            List<NotaMedica> medicamentos = notaRepository.findByPacienteAndTipo(paciente, "MEDICAMENTO");
            List<NotaMedica> consejos = notaRepository.findByPacienteAndTipo(paciente, "CONSEJO");
            
            Map<String, Object> response = new HashMap<>();
            response.put("diagnosticos", diagnosticos);
            response.put("recomendaciones", recomendaciones);
            response.put("medicamentos", medicamentos);
            response.put("consejos", consejos);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Personal clínico + Archivo Clínico + Dirección: ver notas de un paciente
    @GetMapping("/doctor/paciente/{pacienteId}")
    @PreAuthorize(NotaMedicaController.ROLES_LECTURA)
    public ResponseEntity<?> getNotasPorPaciente(@PathVariable Long pacienteId) {
        try {
            // Asignación: el personal clínico solo consulta notas de SUS propios pacientes
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario lector = usuarioRepository.findByUsername(username).orElse(null);
            if (lector != null && lector.getRol() != null
                    && List.of("DOCTOR", "PSICOLOGO", "PSIQUIATRA", "ENFERMERO", "TRABAJADOR_SOCIAL").contains(lector.getRol())) {
                boolean esPropio = citaRepository.findByDoctorId(lector.getId()).stream()
                        .anyMatch(c -> c.getCliente() != null && Long.valueOf(pacienteId).equals(c.getCliente().getId()));
                if (!esPropio) {
                    return ResponseEntity.status(403).body("Este paciente no está asignado a ti");
                }
            }

            List<NotaMedica> notas = notaRepository.findByPacienteId(pacienteId);
            return ResponseEntity.ok(notas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}