package mx.com.ubam.isc.apijwt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Servicio;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Expediente;
import mx.com.ubam.isc.apijwt.model.CasoEtica;
import mx.com.ubam.isc.apijwt.model.Queja;
import mx.com.ubam.isc.apijwt.model.RegistroAuditoria;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.TokenInvalidoRepository;
import mx.com.ubam.isc.apijwt.repository.ServicioRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.ExpedienteRepository;
import mx.com.ubam.isc.apijwt.repository.CasoEticaRepository;
import mx.com.ubam.isc.apijwt.repository.QuejaRepository;
import mx.com.ubam.isc.apijwt.repository.RegistroAuditoriaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private CasoEticaRepository casoEticaRepository;

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private RegistroAuditoriaRepository auditoriaRepository;

    @Override
    public void run(String... args) throws Exception {
        // Limpiar tokens antiguos
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);
        tokenInvalidoRepository.deleteTokensAntiguos(hace7Dias);
        System.out.println("✅ Tokens antiguos eliminados");
        
        // ============================================================
        // 1. USUARIOS ADMINISTRATIVOS
        // ============================================================
        
        // ADMIN - Administrador del sistema
        crearUsuarioSiNoExiste("admin", "123456", "ADMIN", "Administrador del Sistema", "ADMINISTRACION");
        
        // DIRECTOR - Director del Hospital
        crearUsuarioSiNoExiste("director", "123456", "DIRECTOR", "Dr. Director General", "ADMINISTRACION");
        
        // ============================================================
        // 2. JEFES DE DEPARTAMENTO
        // ============================================================
        
        // JEFE_PSICOLOGIA - Jefe del Departamento de Psicología
        crearUsuarioSiNoExiste("jefe.psicologia", "123456", "JEFE_PSICOLOGIA", "Dra. Jefa de Psicología", "PSICOLOGIA");
        
        // JEFE_PSIQUIATRIA - Jefe del Departamento de Psiquiatría
        crearUsuarioSiNoExiste("jefe.psiquiatria", "123456", "JEFE_PSIQUIATRIA", "Dr. Jefe de Psiquiatría", "PSIQUIATRIA");
        
        // JEFE_TRABAJO_SOCIAL - Jefe del Departamento de Trabajo Social
        crearUsuarioSiNoExiste("jefe.ts", "123456", "JEFE_TRABAJO_SOCIAL", "Lic. Jefe de Trabajo Social", "TRABAJO_SOCIAL");
        
        // JEFE_ENFERMERIA - Jefe del Departamento de Enfermería
        crearUsuarioSiNoExiste("jefe.enfermeria", "123456", "JEFE_ENFERMERIA", "Enf. Jefe de Enfermería", "ENFERMERIA");
        
        // JEFE_ARCHIVO - Jefe del Departamento de Archivo Clínico
        crearUsuarioSiNoExiste("jefe.archivo", "123456", "JEFE_ARCHIVO", "Lic. Jefe de Archivo Clínico", "ARCHIVO");
        
        // ============================================================
        // 3. COMITÉ DE ÉTICA
        // ============================================================
        
        crearUsuarioSiNoExiste("comite.etica", "123456", "COMITE_ETICA", "Comité de Ética", "ADMINISTRACION");
        
        // ============================================================
        // 4. PERSONAL DE PSICOLOGÍA
        // ============================================================
        
        crearUsuarioSiNoExiste("psicologo1", "123456", "PSICOLOGO", "Lic. Psicólogo Clínico", "PSICOLOGIA", "Psicología Clínica");
        crearUsuarioSiNoExiste("psicologo2", "123456", "PSICOLOGO", "Lic. Psicólogo Infantil", "PSICOLOGIA", "Psicología Infantil");
        crearUsuarioSiNoExiste("psicologo3", "123456", "PSICOLOGO", "Lic. Terapeuta de Pareja", "PSICOLOGIA", "Terapia de Pareja");
        
        // ============================================================
        // 5. PERSONAL DE PSIQUIATRÍA
        // ============================================================
        
        crearUsuarioSiNoExiste("psiquiatra1", "123456", "PSIQUIATRA", "Dr. Psiquiatra General", "PSIQUIATRIA", "Psiquiatría");
        crearUsuarioSiNoExiste("psiquiatra2", "123456", "PSIQUIATRA", "Dra. Psiquiatra Infantil", "PSIQUIATRIA", "Psiquiatría Infantil");
        crearUsuarioSiNoExiste("psiquiatra3", "123456", "PSIQUIATRA", "Dr. Neuropsiquiatra", "PSIQUIATRIA", "Neuropsiquiatría");
        
        // ============================================================
        // 6. PERSONAL DE ENFERMERÍA
        // ============================================================
        
        crearUsuarioSiNoExiste("enfermero1", "123456", "ENFERMERO", "Enf. María González", "ENFERMERIA");
        crearUsuarioSiNoExiste("enfermero2", "123456", "ENFERMERO", "Enf. José Ramírez", "ENFERMERIA");
        crearUsuarioSiNoExiste("enfermero3", "123456", "ENFERMERO", "Enf. Laura Sánchez", "ENFERMERIA");
        
        // ============================================================
        // 7. PERSONAL DE TRABAJO SOCIAL
        // ============================================================
        
        crearUsuarioSiNoExiste("ts1", "123456", "TRABAJADOR_SOCIAL", "Lic. Ana Torres", "TRABAJO_SOCIAL");
        crearUsuarioSiNoExiste("ts2", "123456", "TRABAJADOR_SOCIAL", "Lic. Carlos Méndez", "TRABAJO_SOCIAL");
        
        // ============================================================
        // 8. PERSONAL ADMINISTRATIVO
        // ============================================================
        
        crearUsuarioSiNoExiste("admin1", "123456", "ADMINISTRATIVO", "Sra. Patricia López", "ADMINISTRACION");
        crearUsuarioSiNoExiste("admin2", "123456", "ADMINISTRATIVO", "Sr. Roberto Flores", "ADMINISTRACION");
        
        // ============================================================
        // 8b. DOCTOR(A) - Karla, con asistente de IA clínico
        // ============================================================
        
        crearUsuarioSiNoExiste("karla", "123456", "DOCTOR", "Dra. Karla Herrera", "PSICOLOGIA", "Psicología Clínica");
        
        // ============================================================
        // 9. EMPLEADO (secretaria/recepción) - se mantiene
        // ============================================================
        
        crearUsuarioSiNoExiste("empleado", "123456", "EMPLEADO", "Empleado Administrativo", "ADMINISTRACION");
        
        // ============================================================
        // 10. PACIENTES DE PRUEBA
        // ============================================================
        
        crearUsuarioSiNoExiste("paciente1", "123456", "PACIENTE", "Juan Pérez", null);
        crearUsuarioSiNoExiste("paciente2", "123456", "PACIENTE", "María García", null);
        crearUsuarioSiNoExiste("paciente3", "123456", "PACIENTE", "Carlos López", null);
        
        // ============================================================
        // 11. SERVICIOS
        // ============================================================
        
        // Catálogo realista de hospital psiquiátrico
        crearServicioSiNoExiste("Consulta Inicial", "Primera valoración con el especialista, incluye historia clínica", 350.0, 45);
        crearServicioSiNoExiste("Valoración Psicológica", "Evaluación diagnóstica con pruebas psicométricas", 450.0, 60);
        crearServicioSiNoExiste("Valoración Psiquiátrica", "Evaluación clínica con psiquiatra y plan de medicación", 600.0, 45);
        crearServicioSiNoExiste("Consulta de Seguimiento Psiquiátrico", "Control de tratamiento farmacológico", 300.0, 30);
        crearServicioSiNoExiste("Terapia Psicológica Individual", "Sesión de terapia cognitivo-conductual", 400.0, 50);
        crearServicioSiNoExiste("Terapia de Seguimiento", "Sesión de continuidad terapéutica", 350.0, 45);
        crearServicioSiNoExiste("Terapia de Pareja", "Sesión de terapia para parejas", 500.0, 60);
        crearServicioSiNoExiste("Terapia Familiar", "Sesión con la familia del paciente", 500.0, 60);
        crearServicioSiNoExiste("Terapia Infantil y del Adolescente", "Terapia especializada para menores de edad", 450.0, 45);
        crearServicioSiNoExiste("Valoración Neuropsicológica", "Batería completa de evaluación neuropsicológica", 900.0, 90);
        crearServicioSiNoExiste("Urgencia Psiquiátrica", "Atención de crisis o urgencia psiquiátrica", 800.0, 60);
        crearServicioSiNoExiste("Hospitalización", "Ingreso a piso de hospitalización psiquiátrica con estancia diaria", 3500.0, 480);
        crearServicioSiNoExiste("Electroconvulsivoterapia (ECT)", "Sesión de ECT con anestesiólogo", 4000.0, 60);
        
        // ============================================================
        // 12. REPARACIÓN DE CONTRASEÑAS CORRUPTAS (bug BCrypt)
        // ============================================================
        repararHashesCorruptos();
        
        // ============================================================
        // 13. DATOS CLÍNICOS INICIALES (expedientes, ética, auditoría)
        // ============================================================
        sembrarDatosClinicos();
        
        System.out.println("========================================");
        System.out.println("✅ TODOS LOS DATOS CARGADOS CORRECTAMENTE");
        System.out.println("========================================");
        System.out.println("📝 USUARIOS CREADOS:");
        System.out.println("   - ADMIN: admin / 123456");
        System.out.println("   - DIRECTOR: director / 123456");
        System.out.println("   - JEFE PSICOLOGÍA: jefe.psicologia / 123456");
        System.out.println("   - JEFE PSIQUIATRÍA: jefe.psiquiatria / 123456");
        System.out.println("   - JEFE TRABAJO SOCIAL: jefe.ts / 123456");
        System.out.println("   - JEFE ENFERMERÍA: jefe.enfermeria / 123456");
        System.out.println("   - JEFE ARCHIVO: jefe.archivo / 123456");
        System.out.println("   - COMITÉ ÉTICA: comite.etica / 123456");
        System.out.println("   - PSICÓLOGO: psicologo1 / 123456");
        System.out.println("   - PSIQUIATRA: psiquiatra1 / 123456");
        System.out.println("   - ENFERMERO: enfermero1 / 123456");
        System.out.println("   - TRABAJADOR SOCIAL: ts1 / 123456");
        System.out.println("   - ADMINISTRATIVO: admin1 / 123456");
        System.out.println("   - EMPLEADO: empleado / 123456");
        System.out.println("   - DOCTORA: karla / 123456");
        System.out.println("   - PACIENTE: paciente1 / 123456");
        System.out.println("========================================");
    }
    
    /**
     * Crea un usuario si no existe (sin especialidad)
     */
    private void crearUsuarioSiNoExiste(String username, String password, String rol, String nombreCompleto, String departamento) {
        crearUsuarioSiNoExiste(username, password, rol, nombreCompleto, departamento, null);
    }
    
    /**
     * Crea un usuario si no existe (con especialidad opcional)
     */
    private void crearUsuarioSiNoExiste(String username, String password, String rol, String nombreCompleto, String departamento, String especialidad) {
        if (usuarioRepository.findByUsername(username).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setRol(rol);
            usuario.setNombreCompleto(nombreCompleto);
            if (departamento != null && !departamento.isEmpty()) {
                usuario.setDepartamento(departamento);
            }
            if (especialidad != null && !especialidad.isEmpty()) {
                usuario.setEspecialidad(especialidad);
            }
            usuarioRepository.save(usuario);
            System.out.println("✅ Usuario creado: " + username + " - " + rol + " - " + nombreCompleto);
        } else {
            System.out.println("ℹ️ Usuario ya existe: " + username);
        }
    }
    
    // Formato oficial de un hash BCrypt: $2(a|y|b)$NN$ + 53 caracteres (60 en total)
    private static final java.util.regex.Pattern BCRYPT_PATTERN =
            java.util.regex.Pattern.compile("\\A\\$2(a|y|b)?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

    /**
     * Repara usuarios cuyo hash de contraseña no es BCrypt válido
     * (hashes insertados a mano, truncados o con caracteres extra).
     * Restablece "123456".
     */
    private void repararHashesCorruptos() {
        List<Usuario> todos = usuarioRepository.findAll();
        for (Usuario u : todos) {
            String hash = u.getPassword();
            if (hash == null || !BCRYPT_PATTERN.matcher(hash).matches()) {
                u.setPassword(passwordEncoder.encode("123456"));
                usuarioRepository.save(u);
                System.out.println("🔧 Contraseña reparada (BCrypt) para usuario: " + u.getUsername());
            }
        }
    }

    /**
     * Siembra datos clínicos reales: expediente por paciente,
     * casos de ética, quejas y primer registro de auditoría.
     */
    private void sembrarDatosClinicos() {
        try {
            // Expediente ACTIVO para cada paciente que aún no tenga uno
            for (Cliente paciente : clienteRepository.findAll()) {
                if (expedienteRepository.findByPacienteId(paciente.getId()).isEmpty()) {
                    Expediente expediente = new Expediente();
                    expediente.setPaciente(paciente);
                    expediente.setEstado("ACTIVO");
                    expediente.setObservaciones("Expediente abierto al ingreso del paciente");
                    expediente.setFechaActualizacion(LocalDateTime.now());
                    expedienteRepository.save(expediente);
                    System.out.println("✅ Expediente creado para paciente: " + paciente.getNombre());
                }
            }
            
            List<Cliente> pacientes = clienteRepository.findAll();
            
            // Casos de ética de ejemplo
            if (casoEticaRepository.count() == 0 && !pacientes.isEmpty()) {
                String[][] casos = {
                    {"Evaluación Psiquiátrica", "Caso enviado por el comité para validar consentimiento informado del tratamiento", "PENDIENTE"},
                    {"Confidencialidad", "Posible divulgación de información clínica entre familiares sin autorización", "EN_REVISION"},
                    {"Tratamiento en Menores", "Solicitud de tratamiento farmacológico para menor de edad con consentimiento de tutor", "PENDIENTE"}
                };
                for (int i = 0; i < casos.length && i < pacientes.size(); i++) {
                    CasoEtica caso = new CasoEtica();
                    caso.setPaciente(pacientes.get(i % pacientes.size()));
                    caso.setTipo(casos[i][0]);
                    caso.setDescripcion(casos[i][1]);
                    caso.setFecha(LocalDate.now().minusDays(i + 1));
                    caso.setEstado(casos[i][2]);
                    casoEticaRepository.save(caso);
                }
                System.out.println("✅ Casos de ética de ejemplo creados");
            }
            
            // Quejas de ejemplo
            if (quejaRepository.count() == 0 && !pacientes.isEmpty()) {
                String[][] quejas = {
                    {"Demora en atención", "El paciente espera más de 30 minutos pasada su hora de cita", "PENDIENTE"},
                    {"Falta de seguimiento", "No se recibió llamada de recordatorio de la cita de control", "EN_PROCESO"},
                    {"Facturación", "Cobro de consulta con precio diferente al publicado", "RESUELTO"}
                };
                for (int i = 0; i < quejas.length && i < pacientes.size(); i++) {
                    Queja queja = new Queja();
                    queja.setPaciente(pacientes.get(i % pacientes.size()));
                    queja.setAsunto(quejas[i][0]);
                    queja.setDescripcion(quejas[i][1]);
                    queja.setFecha(LocalDate.now().minusDays(i + 1));
                    queja.setEstado(quejas[i][2]);
                    if ("RESUELTO".equals(quejas[i][2])) {
                        queja.setResolucion("Se aplicó reembolso de la diferencia y actualización de la lista de precios");
                        queja.setFechaResolucion(LocalDateTime.now().minusDays(1));
                    }
                    quejaRepository.save(queja);
                }
                System.out.println("✅ Quejas de ejemplo creadas");
            }
            
            // Primer registro de auditoría
            if (auditoriaRepository.count() == 0) {
                RegistroAuditoria registro = new RegistroAuditoria();
                registro.setUsuario("SISTEMA");
                registro.setAccion("LOGIN");
                registro.setDetalle("Bitácora de auditoría inicializada con la carga de datos del sistema");
                registro.setFecha(LocalDateTime.now());
                auditoriaRepository.save(registro);
                System.out.println("✅ Bitácora de auditoría inicializada");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error sembrando datos clínicos: " + e.getMessage());
        }
    }

    /**
     * Crea un servicio si no existe
     */
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