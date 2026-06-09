package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.dto.AuthRequest;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.TokenInvalido;
import mx.com.ubam.isc.apijwt.repository.TokenInvalidoRepository;
import mx.com.ubam.isc.apijwt.security.JwtUtil;
import mx.com.ubam.isc.apijwt.service.UsuarioService;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private TokenInvalidoRepository tokenInvalidoRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        System.out.println("=== INTENTO DE LOGIN ===");
        System.out.println("Usuario: " + request.getUsername());
        
        boolean autenticado = usuarioService.autenticar(request.getUsername(), request.getPassword());
        System.out.println("Autenticado: " + autenticado);
        
        if (autenticado) {
            Usuario user = usuarioService.findByUsername(request.getUsername()).get();
            
            // Asegurar que el rol está en mayúsculas
            String rol = user.getRol().toUpperCase();
            String token = jwtUtil.generarToken(request.getUsername(), rol);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("rol", rol);
            response.put("especialidad", user.getEspecialidad());
            
            System.out.println("Token generado para: " + user.getUsername() + " con rol: " + rol);
            return ResponseEntity.ok(response);
        }
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Credenciales inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            System.out.println("=== INTENTO DE LOGOUT ===");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                System.out.println("Token recibido: " + token.substring(0, Math.min(token.length(), 50)) + "...");
                
                String username = jwtUtil.extraerUsuario(token);
                System.out.println("Usuario extraído del token: " + username);
                
                Usuario usuario = usuarioService.findByUsername(username).orElse(null);
                
                if (usuario == null) {
                    System.out.println("⚠️ Usuario no encontrado: " + username);
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Usuario no encontrado");
                    return ResponseEntity.badRequest().body(error);
                }
                
                // Verificar si el token ya está en la blacklist
                if (tokenInvalidoRepository.findByToken(token).isPresent()) {
                    System.out.println("⚠️ Token ya estaba en blacklist para usuario: " + username);
                    Map<String, String> response = new HashMap<>();
                    response.put("mensaje", "Sesión ya cerrada anteriormente");
                    return ResponseEntity.ok(response);
                }
                
                // ✅ AHORA SÍ FUNCIONA - Constructor TokenInvalido(String, Usuario)
                TokenInvalido tokenInvalido = new TokenInvalido(token, usuario);
                tokenInvalidoRepository.save(tokenInvalido);
                
                System.out.println("✅ Token invalidado para usuario: " + username);
                Map<String, String> response = new HashMap<>();
                response.put("mensaje", "Sesión cerrada correctamente");
                return ResponseEntity.ok(response);
            }
            
            System.out.println("⚠️ No se proporcionó token válido");
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se pudo cerrar sesión: Token no proporcionado");
            return ResponseEntity.badRequest().body(error);
            
        } catch (Exception e) {
            System.err.println("❌ Error al cerrar sesión: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al cerrar sesión: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}