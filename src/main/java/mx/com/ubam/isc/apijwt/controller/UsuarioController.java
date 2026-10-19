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
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Gestión de usuarios del consultorio.
 * ADMIN y DIRECTOR pueden crear/eliminar personal; el DIRECTOR no puede manipular cuentas ADMIN.
 */

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

    @Autowired
    private AuditoriaService auditoriaService;

    // Whitelist completa de roles del organigrama del consultorio
    private static final List<String> ROLES_VALIDOS = List.of(
        "ADMIN", "DIRECTOR", "COMITE_ETICA",
        "JEFE_PSICOLOGIA", "JEFE_PSIQUIATRIA", "JEFE_TRABAJO_SOCIAL", "JEFE_ENFERMERIA", "JEFE_ARCHIVO",
        "PSICOLOGO", "PSIQUIATRA", "ENFERMERO", "TRABAJADOR_SOCIAL",
        "ADMINISTRATIVO", "EMPLEADO", "PACIENTE"
    );

    // Roles clínicos que requieren especialidad obligatoria
    private static final List<String> ROLES_CON_ESPECIALIDAD = List.of("PSICOLOGO", "PSIQUIATRA", "DOCTOR");

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
    @PreAuthorize("hasRole('ADMIN') or hasRole('DIRECTOR')")
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
            
            String rolSolicitado = usuario.getRol() != null ? usuario.getRol().toUpperCase() : null;
            if (rolSolicitado == null || !ROLES_VALIDOS.contains(rolSolicitado)) {
                return ResponseEntity.badRequest().body("Rol no válido. Roles permitidos: " + String.join(", ", ROLES_VALIDOS));
            }
            usuario.setRol(rolSolicitado);
            
            if (ROLES_CON_ESPECIALIDAD.contains(rolSolicitado) && 
                (usuario.getEspecialidad() == null || usuario.getEspecialidad().isEmpty())) {
                return ResponseEntity.badRequest().body("Para un " + rolSolicitado + " la especialidad es obligatoria");
            }
            
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            Usuario nuevo = usuarioRepository.save(usuario);
            
            System.out.println("✅ Usuario creado: " + nuevo.getUsername() + " - Rol: " + nuevo.getRol());
            auditoriaService.registrar(AuditoriaService.usuarioActual(), "USUARIO",
                "Creación de usuario '" + nuevo.getUsername() + "' con rol " + nuevo.getRol());
            
            return ResponseEntity.ok(nuevo);
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear usuario: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error al crear usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DIRECTOR')")
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
            
            // El DIRECTOR no puede eliminar cuentas ADMIN ni a sí mismo
            String usuarioActual = AuditoriaService.usuarioActual();
            if ("ADMIN".equals(rol) && !usuarioActual.equals(username)) {
                return ResponseEntity.status(403).body("Solo el ADMIN puede eliminar otras cuentas ADMIN");
            }
            if (username.equals(usuarioActual)) {
                return ResponseEntity.badRequest().body("No puedes eliminar tu propia cuenta mientras tienes sesión activa");
            }
            
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
            auditoriaService.registrar(usuarioActual, "USUARIO",
                "Eliminación de usuario '" + username + "' (rol " + rol + ")");
            
            return ResponseEntity.ok("Usuario '" + username + "' eliminado correctamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error al eliminar usuario: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al eliminar usuario: " + e.getMessage());
        }
    }
}