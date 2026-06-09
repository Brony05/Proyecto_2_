package mx.com.ubam.isc.apijwt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Servicio;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.TokenInvalidoRepository;
import mx.com.ubam.isc.apijwt.repository.ServicioRepository;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private TokenInvalidoRepository tokenInvalidoRepository;
    
    @Autowired
    private ServicioRepository servicioRepository;

    @Override
    public void run(String... args) throws Exception {
        // Limpiar tokens antiguos
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);
        tokenInvalidoRepository.deleteTokensAntiguos(hace7Dias);
        System.out.println("✅ Tokens antiguos eliminados");
        
        // Crear o actualizar usuario ADMIN con contraseña 123456
        Usuario admin = usuarioRepository.findByUsername("admin").orElse(new Usuario());
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));  // ← Contraseña: 123456
        admin.setRol("ADMIN");
        usuarioRepository.save(admin);
        System.out.println("✅ Usuario ADMIN: admin / 123456");
        
        // Crear o actualizar usuario EMPLEADO con contraseña 123456
        Usuario empleado = usuarioRepository.findByUsername("empleado").orElse(new Usuario());
        empleado.setUsername("empleado");
        empleado.setPassword(passwordEncoder.encode("123456"));  // ← Contraseña: 123456
        empleado.setRol("EMPLEADO");
        usuarioRepository.save(empleado);
        System.out.println("✅ Usuario EMPLEADO: empleado / 123456");
        
        // Crear servicios predefinidos
        crearServicioSiNoExiste("Consulta Inicial", "Primera consulta con el especialista", 800.0, 45);
        crearServicioSiNoExiste("Terapia Psicológica", "Sesión de terapia individual", 700.0, 50);
        crearServicioSiNoExiste("Seguimiento", "Seguimiento de tratamiento", 500.0, 30);
        crearServicioSiNoExiste("Urgencia Psiquiátrica", "Atención de urgencia", 1200.0, 60);
        crearServicioSiNoExiste("Valoración", "Valoración inicial del caso", 600.0, 40);
        crearServicioSiNoExiste("Terapia de Pareja", "Sesión de terapia para parejas", 900.0, 60);
        crearServicioSiNoExiste("Psicología Infantil", "Terapia para niños y adolescentes", 750.0, 45);
        crearServicioSiNoExiste("Orientación Vocacional", "Orientación para elección de carrera", 550.0, 50);
        
        System.out.println("========================================");
        System.out.println("✅ TODOS LOS DATOS CARGADOS CORRECTAMENTE");
        System.out.println("📝 CONTRASEÑAS: admin / 123456  |  empleado / 123456");
        System.out.println("========================================");
    }
    
    private void crearServicioSiNoExiste(String nombre, String descripcion, double precio, int duracion) {
        if (servicioRepository.findByNombre(nombre).isEmpty()) {
            Servicio servicio = new Servicio();
            servicio.setNombre(nombre);
            servicio.setDescripcion(descripcion);
            servicio.setPrecio(precio);
            servicio.setDuracionMinutos(duracion);
            servicio.setActivo(true);
            servicioRepository.save(servicio);
            System.out.println("✅ Servicio creado: " + nombre + " - $" + precio);
        }
    }
}