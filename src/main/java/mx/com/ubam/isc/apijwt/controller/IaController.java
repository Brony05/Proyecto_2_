package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.service.IaService;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/ia")
public class IaController {

    @Autowired
    private IaService iaService;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("ollamaDisponible", iaService.isOllamaDisponible());
        response.put("mensaje", iaService.isOllamaDisponible() ? 
            "Ollama está disponible" : 
            "Ollama no está disponible. Ejecuta: ollama run llama3.2:3b");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recomendar-tratamiento")
    public ResponseEntity<?> recomendarTratamiento(@RequestBody Map<String, Object> payload) {
        try {
            String diagnostico = (String) payload.get("diagnostico");
            String sintomas = (String) payload.getOrDefault("sintomas", "");
            String historial = (String) payload.getOrDefault("historial", "");
            
            if (diagnostico == null || diagnostico.isEmpty()) {
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
            
            if (diagnostico == null || diagnostico.isEmpty()) {
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
            String categoriaFinal = (categoria != null && !categoria.isEmpty()) ? categoria : "manejo del estrés";
            String consejos = iaService.generarConsejos(categoriaFinal);
            return ResponseEntity.ok(consejos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        try {
            String mensaje = (String) payload.get("mensaje");
            String contexto = (String) payload.getOrDefault("contexto", "");
            
            if (mensaje == null || mensaje.isEmpty()) {
                return ResponseEntity.badRequest().body("El mensaje es requerido");
            }
            
            String respuesta = iaService.chatRespuesta(mensaje, contexto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("respuesta", respuesta);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}