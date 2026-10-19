package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.NotaMedica;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.NotaMedicaRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.service.IaService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bot de IA del consultorio.
 * SOLO los doctores (DOCTOR) y los pacientes (PACIENTE) tienen acceso.
 * Cada rol recibe un asistente distinto: clínico para el doctor, empático para el paciente.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/ia")
@PreAuthorize("hasAnyRole('DOCTOR', 'PACIENTE')")
public class IaController {

    @Autowired
    private IaService iaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private NotaMedicaRepository notaRepository;

    private boolean esDoctor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_DOCTOR".equals(a.getAuthority())) return true;
        }
        return false;
    }

    /**
     * Contexto clínico del PACIENTE autenticado, tomado de SUS notas médicas
     * (nunca de datos enviados por el cliente).
     */
    private String contextoDelPaciente() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            String claveCorreo = usuario != null ? usuario.getUsername() : username;
            Cliente paciente = clienteRepository.findByCorreo(claveCorreo);
            if (paciente == null) {
                paciente = clienteRepository.findByCorreoIgnoreCase(claveCorreo).orElse(null);
            }
            if (paciente == null) return "";
            List<NotaMedica> diagnosticos = notaRepository.findByPacienteAndTipo(paciente, "DIAGNOSTICO");
            List<NotaMedica> recomendaciones = notaRepository.findByPacienteAndTipo(paciente, "RECOMENDACION");
            StringBuilder sb = new StringBuilder();
            if (!diagnosticos.isEmpty()) {
                sb.append("Diagnóstico actual: ").append(diagnosticos.get(0).getDiagnostico()).append(". ");
            }
            if (!recomendaciones.isEmpty()) {
                sb.append("Última recomendación médica: ")
                  .append(recomendaciones.get(0).getRecomendaciones()).append(".");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean disponible = iaService.isOllamaDisponible();
        Map<String, Object> response = new HashMap<>();
        response.put("ollamaDisponible", disponible);
        response.put("esDoctor", esDoctor());
        response.put("mensaje", disponible
                ? (esDoctor() ? "Asistente clínico listo" : "Tu asistente de bienestar está listo")
                : "El servicio de IA no está disponible. Ejecuta: ollama run llama3.2:3b");
        return ResponseEntity.ok(response);
    }

    /** Plan clínico generado por IA: SOLO para el doctor. */
    @PostMapping("/recomendar-tratamiento")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> recomendarTratamiento(@RequestBody Map<String, Object> payload) {
        try {
            String diagnostico = (String) payload.get("diagnostico");
            String sintomas = (String) payload.getOrDefault("sintomas", "");
            String historial = (String) payload.getOrDefault("historial", "");

            if (diagnostico == null || diagnostico.isBlank()) {
                return ResponseEntity.badRequest().body("El diagnóstico es requerido");
            }

            String recomendacion = iaService.generarRecomendacionTratamiento(diagnostico, sintomas, historial);

            Map<String, Object> response = new HashMap<>();
            response.put("recomendacion", recomendacion);
            response.put("diagnostico", diagnostico);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/explicar-diagnostico")
    public ResponseEntity<?> explicarDiagnostico(@RequestBody Map<String, Object> payload) {
        try {
            String diagnostico = (String) payload.get("diagnostico");
            String nivelDetalle = (String) payload.getOrDefault("nivelDetalle", "simple");

            if (diagnostico == null || diagnostico.isBlank()) {
                return ResponseEntity.badRequest().body("El diagnóstico es requerido");
            }

            String explicacion = iaService.explicarDiagnostico(diagnostico, nivelDetalle);

            Map<String, Object> response = new HashMap<>();
            response.put("explicacion", explicacion);
            response.put("diagnostico", diagnostico);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/consejos")
    public ResponseEntity<?> obtenerConsejos(@RequestParam(required = false) String categoria) {
        try {
            String categoriaFinal = (categoria != null && !categoria.isBlank()) ? categoria : "manejo del estrés";
            return ResponseEntity.ok(iaService.generarConsejos(categoriaFinal));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** Chat del bot: cada rol habla con su propio asistente. */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        try {
            String mensaje = (String) payload.get("mensaje");
            String contextoCliente = (String) payload.getOrDefault("contexto", "");

            if (mensaje == null || mensaje.isBlank()) {
                return ResponseEntity.badRequest().body("El mensaje es requerido");
            }

            boolean doctor = esDoctor();
            String contexto = doctor
                    ? (contextoCliente == null || contextoCliente.isBlank() ? "Consulta clínica general" : contextoCliente)
                    : contextoDelPaciente();

            String respuesta = iaService.chatRespuesta(mensaje, contexto, doctor);

            Map<String, Object> response = new HashMap<>();
            response.put("respuesta", respuesta);
            response.put("asistente", doctor ? "Asistente clínico" : "Asistente de bienestar");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
