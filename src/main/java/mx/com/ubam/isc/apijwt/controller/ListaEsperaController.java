package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.*;
import mx.com.ubam.isc.apijwt.repository.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lista-espera")
public class ListaEsperaController {

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;

    @PostMapping
    public ResponseEntity<?> agregarAListaEspera(@RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Cliente paciente = clienteRepository.findByCorreo(username);
            
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }
            
            String servicioInteres = (String) payload.get("servicioInteres");
            
            if (servicioInteres == null || servicioInteres.isEmpty()) {
                return ResponseEntity.badRequest().body("Debes seleccionar un servicio de interés");
            }
            
            if (listaEsperaRepository.existsByPacienteAndServicioInteres(paciente, servicioInteres)) {
                return ResponseEntity.badRequest().body("Ya estás en lista de espera para este servicio");
            }
            
            ListaEspera lista = new ListaEspera();
            lista.setPaciente(paciente);
            lista.setServicioInteres(servicioInteres);
            lista.setFechaRegistro(LocalDateTime.now());
            lista.setNotificado(false);
            
            if (payload.containsKey("fechaDeseada") && payload.get("fechaDeseada") != null) {
                lista.setFechaDeseada(LocalDate.parse((String) payload.get("fechaDeseada")));
            }
            if (payload.containsKey("horarioPreferido") && payload.get("horarioPreferido") != null) {
                lista.setHorarioPreferido((String) payload.get("horarioPreferido"));
            }
            
            listaEsperaRepository.save(lista);
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Te has agregado a la lista de espera. Te notificaremos cuando haya disponibilidad.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<?> getMisSolicitudes() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Cliente paciente = clienteRepository.findByCorreo(username);
            
            if (paciente == null) {
                return ResponseEntity.badRequest().body("Paciente no encontrado");
            }
            
            List<ListaEspera> solicitudes = listaEsperaRepository.findByPaciente(paciente);
            return ResponseEntity.ok(solicitudes);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSolicitud(@PathVariable Long id) {
        try {
            listaEsperaRepository.deleteById(id);
            return ResponseEntity.ok("Eliminado de la lista de espera");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}