package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
public class RegistroController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/registro")
    public ResponseEntity<?> registrarPaciente(@RequestBody Map<String, String> datos) {
        try {
            String nombre = datos.get("nombre");
            String telefono = datos.get("telefono");
            String correo = datos.get("correo");
            String username = datos.get("username");
            String password = datos.get("password");
            
            System.out.println("🔵 Intentando registrar paciente:");
            System.out.println("   - Nombre: " + nombre);
            System.out.println("   - Correo: " + correo);
            System.out.println("   - Username elegido: " + username);
            
            // Validaciones
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre de usuario es obligatorio");
            }
            if (password == null || password.length() < 4) {
                return ResponseEntity.badRequest().body("La contraseña debe tener al menos 4 caracteres");
            }
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre es obligatorio");
            }
            if (correo == null || !correo.contains("@")) {
                return ResponseEntity.badRequest().body("El correo electrónico no es válido");
            }
            
            // Validación: Usuario ya existe
            if (usuarioRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body("El nombre de usuario ya está en uso");
            }
            
            // Validación: Correo ya registrado
            Optional<Cliente> clienteExistente = clienteRepository.findByCorreoIgnoreCase(correo);
            if (clienteExistente.isPresent()) {
                return ResponseEntity.badRequest().body("El correo electrónico ya está registrado");
            }
            
            // Crear usuario con rol PACIENTE
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUsername(username);
            nuevoUsuario.setPassword(passwordEncoder.encode(password));
            nuevoUsuario.setRol("PACIENTE");
            usuarioRepository.save(nuevoUsuario);
            System.out.println("✅ Usuario creado: " + username);
            
            // Crear cliente asociado al usuario
            Cliente nuevoCliente = new Cliente();
            nuevoCliente.setNombre(nombre);
            nuevoCliente.setTelefono(telefono);
            nuevoCliente.setCorreo(correo);
            clienteRepository.save(nuevoCliente);
            System.out.println("✅ Cliente creado: " + nombre);
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Registro exitoso. Ahora puedes iniciar sesión con tu usuario: " + username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error en registro: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error en el registro: " + e.getMessage());
        }
    }
}