package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.TokenInvalido;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.TokenInvalidoRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private TokenInvalidoRepository tokenInvalidoRepository;

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<Usuario> usuarios = usuarioRepository.findAll();
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar usuarios: " + e.getMessage());
        }
    }

    @GetMapping("/doctores")
    public ResponseEntity<?> listarDoctores() {
        try {
            List<Usuario> doctores = usuarioRepository.findByRol("DOCTOR");
            return ResponseEntity.ok(doctores);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario usuario) {
        try {
            System.out.println("🔵 Creando nuevo usuario: " + usuario.getUsername());
            
            if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("El usuario '" + usuario.getUsername() + "' ya existe");
            }
            
            if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre de usuario es obligatorio");
            }
            
            if (usuario.getPassword() == null || usuario.getPassword().length() < 4) {
                return ResponseEntity.badRequest().body("La contraseña debe tener al menos 4 caracteres");
            }
            
            if (usuario.getRol() == null || (!usuario.getRol().equals("ADMIN") && 
                !usuario.getRol().equals("EMPLEADO") && !usuario.getRol().equals("DOCTOR"))) {
                return ResponseEntity.badRequest().body("El rol debe ser ADMIN, EMPLEADO o DOCTOR");
            }
            
            if ("DOCTOR".equals(usuario.getRol()) && 
                (usuario.getEspecialidad() == null || usuario.getEspecialidad().isEmpty())) {
                return ResponseEntity.badRequest().body("Para un DOCTOR la especialidad es obligatoria");
            }
            
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            Usuario nuevo = usuarioRepository.save(usuario);
            
            System.out.println("✅ Usuario creado: " + nuevo.getUsername() + " - Rol: " + nuevo.getRol());
            
            return ResponseEntity.ok(nuevo);
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error al crear usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String username = usuario.getUsername();
            String rol = usuario.getRol();
            
            System.out.println("🔵 Eliminando usuario: " + username + " (Rol: " + rol + ")");
            
            tokenInvalidoRepository.deleteByUsuario(usuario);
            System.out.println("✅ Tokens invalidados para: " + username);
            
            if ("PACIENTE".equals(rol)) {
                Optional<Cliente> clienteOpt = clienteRepository.findByCorreoIgnoreCase(username);
                if (clienteOpt.isPresent()) {
                    Cliente cliente = clienteOpt.get();
                    List<Cita> citas = citaRepository.findByClienteId(cliente.getId());
                    citaRepository.deleteAll(citas);
                    System.out.println("✅ Citas eliminadas para: " + username);
                    clienteRepository.deleteById(cliente.getId());
                    System.out.println("✅ Cliente eliminado para: " + username);
                }
            }
            
            usuarioRepository.deleteById(id);
            System.out.println("✅ Usuario eliminado: " + username);
            
            return ResponseEntity.ok("Usuario '" + username + "' eliminado correctamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error al eliminar usuario: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al eliminar usuario: " + e.getMessage());
        }
    }
}