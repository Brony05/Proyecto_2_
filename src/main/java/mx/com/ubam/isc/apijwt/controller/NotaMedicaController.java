package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.*;
import mx.com.ubam.isc.apijwt.repository.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/notas")
public class NotaMedicaController {

    @Autowired
    private NotaMedicaRepository notaRepository;
    
    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;


 // En NotaMedicaController.java, asegurar que el método crearNota guarda correctamente

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> crearNota(@RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario doctor = usuarioRepository.findByUsername(username).orElse(null);
            
            if (doctor == null) {
                return ResponseEntity.status(401).body("Doctor no autenticado");
            }
            
            Long pacienteId = ((Number) payload.get("pacienteId")).longValue();
            String tipo = (String) payload.get("tipo");
            
            Cliente paciente = clienteRepository.findById(pacienteId).orElse(null);
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }
            
            NotaMedica nota = new NotaMedica();
            nota.setPaciente(paciente);
            nota.setDoctor(doctor);
            nota.setTipo(tipo);
            nota.setFechaCreacion(LocalDateTime.now());
            
            // Guardar según el tipo
            if ("DIAGNOSTICO".equals(tipo)) {
                String diagnostico = (String) payload.get("diagnostico");
                nota.setDiagnostico(diagnostico);
            } else if ("RECOMENDACION".equals(tipo)) {
                String recomendaciones = (String) payload.get("recomendaciones");
                nota.setRecomendaciones(recomendaciones);
            } else if ("MEDICAMENTO".equals(tipo)) {
                String medicamentos = (String) payload.get("medicamentos");
                nota.setMedicamentos(medicamentos);
            } else {
                String consejos = (String) payload.get("recomendaciones");
                nota.setRecomendaciones(consejos);
            }
            
            notaRepository.save(nota);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Nota guardada correctamente");
            response.put("nota", nota);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear nota: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error al crear nota: " + e.getMessage());
        }
    }
    
    
    
    
    
    // Paciente: Ver sus notas médicas
    @GetMapping("/paciente")
    public ResponseEntity<?> getMisNotas() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Cliente paciente = clienteRepository.findByCorreo(username);
            
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
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
    
    // Doctor: Ver notas de sus pacientes
    @GetMapping("/doctor/paciente/{pacienteId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getNotasPorPaciente(@PathVariable Long pacienteId) {
        try {
            List<NotaMedica> notas = notaRepository.findByPacienteId(pacienteId);
            return ResponseEntity.ok(notas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}