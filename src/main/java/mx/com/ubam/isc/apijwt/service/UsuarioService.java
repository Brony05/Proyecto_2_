package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean autenticar(String username, String password) {
        Optional<Usuario> user = usuarioRepository.findByUsername(username);
        if (user.isPresent()) {
            System.out.println("Usuario encontrado: " + user.get().getUsername());
            System.out.println("Rol del usuario: " + user.get().getRol());
            boolean matches = passwordEncoder.matches(password, user.get().getPassword());
            System.out.println("Verificación BCrypt: " + matches);
            return matches;
        }
        System.out.println("Usuario NO encontrado: " + username);
        return false;
    }

    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Usuario guardar(Usuario usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }
    
    public Usuario crearUsuarioAdmin(String username, String password) {
        Usuario admin = new Usuario();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRol("ADMIN");
        return usuarioRepository.save(admin);
    }
    
    public Usuario crearUsuarioEmpleado(String username, String password) {
        Usuario empleado = new Usuario();
        empleado.setUsername(username);
        empleado.setPassword(passwordEncoder.encode(password));
        empleado.setRol("EMPLEADO");
        return usuarioRepository.save(empleado);
    }
    
    public Usuario crearUsuarioPaciente(String username, String password) {
        Usuario paciente = new Usuario();
        paciente.setUsername(username);
        paciente.setPassword(passwordEncoder.encode(password));
        paciente.setRol("PACIENTE");
        return usuarioRepository.save(paciente);
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }
}