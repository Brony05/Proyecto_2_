package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/mensajes")
public class MensajeController {

    @Autowired
    private MensajeRepository mensajeRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Enviar mensaje
    @PostMapping
    public ResponseEntity<?> enviarMensaje(@RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario remitente = usuarioRepository.findByUsername(username).orElse(null);
            
            String destinatarioUsername = (String) payload.get("destinatario");
            String contenido = (String) payload.get("contenido");
            String asunto = (String) payload.get("asunto");
            
            Usuario destinatario = usuarioRepository.findByUsername(destinatarioUsername).orElse(null);
            
            if (destinatario == null) {
                return ResponseEntity.badRequest().body("Destinatario no encontrado");
            }
            
            Mensaje mensaje = new Mensaje();
            mensaje.setRemitente(remitente);
            mensaje.setDestinatario(destinatario);
            mensaje.setContenido(contenido);
            mensaje.setAsunto(asunto);
            mensaje.setFechaEnvio(LocalDateTime.now());
            mensaje.setLeido(false);
            
            mensajeRepository.save(mensaje);
            
            return ResponseEntity.ok(mensaje);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Obtener mensajes recibidos
    @GetMapping("/recibidos")
    public ResponseEntity<?> getMensajesRecibidos() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            
            List<Mensaje> mensajes = mensajeRepository.findByDestinatarioOrderByFechaEnvioDesc(usuario);
            return ResponseEntity.ok(mensajes);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Marcar mensaje como leído
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeido(@PathVariable Long id) {
        try {
            Mensaje mensaje = mensajeRepository.findById(id).orElse(null);
            if (mensaje != null) {
                mensaje.setLeido(true);
                mensajeRepository.save(mensaje);
            }
            return ResponseEntity.ok(mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // Contar mensajes no leídos
    @GetMapping("/no-leidos/count")
    public ResponseEntity<?> countNoLeidos() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            
            Long count = mensajeRepository.countByDestinatarioAndLeidoFalse(usuario);
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}