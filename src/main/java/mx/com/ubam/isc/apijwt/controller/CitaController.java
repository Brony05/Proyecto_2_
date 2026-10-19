package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Cita;import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Servicio;
import mx.com.ubam.isc.apijwt.service.CitaService;
import mx.com.ubam.isc.apijwt.service.EmailService;
import mx.com.ubam.isc.apijwt.service.PdfExportService;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.ServicioRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PdfExportService pdfExportService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private mx.com.ubam.isc.apijwt.repository.CitaRepository citaRepository;
    
    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    private static final List<String> HORARIOS_DISPONIBLES = List.of(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30", "18:00"
    );

    // Roles del personal clínico que atienden citas y pueden iniciarlas/completarlas
    private static final List<String> ROLES_CLINICOS = List.of(
        "DOCTOR", "PSICOLOGO", "PSIQUIATRA", "ENFERMERO", "TRABAJADOR_SOCIAL"
    );

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            
            if (usuario == null) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            
            if ("ADMIN".equals(usuario.getRol()) || "EMPLEADO".equals(usuario.getRol())) {
                return ResponseEntity.ok(citaService.listarTodas());
            }
            
            if ("PACIENTE".equals(usuario.getRol())) {
                Cliente cliente = clienteRepository.findByCorreo(usuario.getUsername());
                if (cliente != null) {
                    return ResponseEntity.ok(citaService.listarPorCliente(cliente.getId()));
                }
                return ResponseEntity.ok(List.of());
            }
            
            // Asignación de pacientes: el personal clínico SOLO ve sus propias citas
            if (ROLES_CLINICOS.contains(usuario.getRol())) {
                return ResponseEntity.ok(citaRepository.findByDoctorId(usuario.getId()));
            }
            
            return ResponseEntity.ok(citaService.listarTodas());
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar citas: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return citaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/servicios")
    public ResponseEntity<?> obtenerServicios() {
        return ResponseEntity.ok(servicioRepository.findByActivoTrueOrderByNombreAsc());
    }

    private Usuario asignarDoctorPorEspecialidad(String servicioNombre, LocalDate fecha, LocalTime hora) {
        String especialidadRequerida;
        switch (servicioNombre) {
            // Psicología Clínica
            case "Consulta Inicial":
            case "Valoración Psicológica":
            case "Terapia Psicológica Individual":
            case "Terapia de Seguimiento":
                especialidadRequerida = "Psicología Clínica"; break;
            // Psiquiatría
            case "Valoración Psiquiátrica":
            case "Consulta de Seguimiento Psiquiátrico":
            case "Urgencia Psiquiátrica":
                especialidadRequerida = "Psiquiatría"; break;
            // Infantil
            case "Terapia Infantil y del Adolescente":
                especialidadRequerida = "Psicología Infantil"; break;
            // Pareja / Familia
            case "Terapia de Pareja":
            case "Terapia Familiar":
                especialidadRequerida = "Terapia de Pareja"; break;
            // Neuropsicología
            case "Valoración Neuropsicológica":
                especialidadRequerida = "Neuropsicología"; break;
            default: especialidadRequerida = "Psicología Clínica";
        }
        
        // 1) Compatibilidad con el modelo anterior: usuarios con rol DOCTOR
        List<Usuario> candidatos = new ArrayList<>(usuarioRepository.findByRolAndEspecialidad("DOCTOR", especialidadRequerida));
        
        // 2) Modelo nuevo por rol/departamento (PSICOLOGO, PSIQUIATRA, etc.)
        if (candidatos.isEmpty()) {
            candidatos = new ArrayList<>(candidatosPorRol(especialidadRequerida));
        }
        if (candidatos.isEmpty()) {
            return null;
        }
        
        // Preferir a quienes tienen la especialidad exacta; si nadie la tiene, todo el pool del rol
        List<Usuario> conEspecialidad = candidatos.stream()
                .filter(u -> especialidadRequerida.equals(u.getEspecialidad()))
                .collect(Collectors.toList());
        List<Usuario> pool = conEspecialidad.isEmpty() ? candidatos : conEspecialidad;
        
        // Elegir al primer especialista libre en el horario solicitado
        for (Usuario candidato : pool) {
            if (citaService.verificarDisponibilidadDoctor(candidato.getId(), fecha, hora)) {
                return candidato;
            }
        }
        
        // Ninguno libre: devolver el primero para que el flujo normal muestre el error de horario
        return pool.get(0);
    }
    
    private List<Usuario> candidatosPorRol(String especialidad) {
        if (especialidad == null) {
            return List.of();
        }
        if (especialidad.contains("Psiquiat")) {
            return usuarioRepository.findByRol("PSIQUIATRA");
        }
        if (especialidad.contains("Neuropsic")) {
            List<Usuario> pool = new ArrayList<>(usuarioRepository.findByRol("PSICOLOGO"));
            pool.addAll(usuarioRepository.findByRol("PSIQUIATRA"));
            return pool;
        }
        if (especialidad.contains("Psicolog") || especialidad.contains("Terapia")) {
            return usuarioRepository.findByRol("PSICOLOGO");
        }
        return List.of();
    }
    
    private String getNombreDoctor(Usuario doctor) {
        if (doctor == null) return "Asignado";
        if (doctor.getNombreCompleto() != null && !doctor.getNombreCompleto().isEmpty()) {
            return doctor.getNombreCompleto();
        }
        return doctor.getUsername();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("🔵 USERNAME: '" + username + "'");
            
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            
            if (usuario == null) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            
            String fechaStr = (String) payload.get("fecha");
            String horaStr = (String) payload.get("hora");
            String servicioNombre = (String) payload.get("servicio");
            
            if (fechaStr == null || fechaStr.isEmpty()) {
                return ResponseEntity.badRequest().body("La fecha es obligatoria");
            }
            if (horaStr == null || horaStr.isEmpty()) {
                return ResponseEntity.badRequest().body("La hora es obligatoria");
            }
            if (servicioNombre == null || servicioNombre.isEmpty()) {
                return ResponseEntity.badRequest().body("El servicio es obligatorio");
            }
            
            LocalDate fecha = LocalDate.parse(fechaStr);
            LocalTime hora = LocalTime.parse(horaStr);
            LocalDate hoy = LocalDate.now();
            LocalTime ahora = LocalTime.now();
            
            if (fecha.isBefore(hoy)) {
                return ResponseEntity.badRequest().body("❌ No se pueden agendar citas en fechas pasadas");
            }
            
            if (fecha.equals(hoy) && hora.isBefore(ahora)) {
                return ResponseEntity.badRequest().body("❌ No se puede agendar una cita para una hora que ya pasó. La hora actual es: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            if (!HORARIOS_DISPONIBLES.contains(horaStr)) {
                return ResponseEntity.badRequest().body("Horario no válido");
            }
            
            Servicio servicio = servicioRepository.findByNombre(servicioNombre).orElse(null);
            Double precio = (servicio != null) ? servicio.getPrecio() : 500.0;
            
            Cliente cliente = null;
            if ("PACIENTE".equals(usuario.getRol())) {
                cliente = clienteRepository.findByCorreo(usuario.getUsername());
                if (cliente == null) {
                    cliente = new Cliente();
                    cliente.setNombre(usuario.getUsername().split("@")[0]);
                    cliente.setCorreo(usuario.getUsername());
                    cliente.setTelefono("Por definir");
                    cliente = clienteRepository.save(cliente);
                }
            } else {
                Map<String, Object> clienteMap = (Map<String, Object>) payload.get("cliente");
                if (clienteMap == null || clienteMap.get("id") == null) {
                    return ResponseEntity.badRequest().body("Debes seleccionar un cliente");
                }
                Long clienteId = ((Number) clienteMap.get("id")).longValue();
                cliente = clienteRepository.findById(clienteId).orElse(null);
                if (cliente == null) {
                    return ResponseEntity.badRequest().body("Cliente no encontrado");
                }
            }
            
            Usuario doctor = asignarDoctorPorEspecialidad(servicioNombre, fecha, hora);
            if (doctor == null) {
                return ResponseEntity.badRequest().body("No hay doctores disponibles para el servicio: " + servicioNombre);
            }
            
            if (!citaService.verificarDisponibilidadDoctor(doctor.getId(), fecha, hora)) {
                return ResponseEntity.badRequest().body("El doctor no está disponible en ese horario");
            }
            
            if (citaService.yaTieneCitaEnFecha(cliente.getId(), fecha)) {
                return ResponseEntity.badRequest().body("Ya tienes una cita agendada para esta fecha. Solo puedes tener una cita por día.");
            }
            
            if ("Consulta Inicial".equals(servicioNombre) && citaService.yaTieneConsultaInicial(cliente.getId())) {
                return ResponseEntity.badRequest().body("Ya has agendado una Consulta Inicial anteriormente.");
            }
            
            // La disponibilidad ya se validó por doctor arriba; se eliminó la validación global deprecada
            
            Cita cita = new Cita();
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setServicio(servicioNombre);
            cita.setPrecio(precio);
            cita.setEstado("AGENDADA");
            cita.setAsistencia("PENDIENTE");
            cita.setCliente(cliente);
            cita.setUsuario(usuario);
            cita.setDoctor(doctor);
            
            if (payload.containsKey("acompananteNombre")) {
                cita.setAcompananteNombre((String) payload.get("acompananteNombre"));
                cita.setAcompananteRelacion((String) payload.get("acompananteRelacion"));
                cita.setAcompananteTelefono((String) payload.get("acompananteTelefono"));
                cita.setAcompananteCorreo((String) payload.get("acompananteCorreo"));
            }
            
            Cita nueva = citaService.crear(cita);
            
            auditoriaService.registrar(usuario.getUsername(), "CREACION",
                "Cita agendada ID " + nueva.getId() + " - " + servicioNombre + " el " + fecha
                + " " + hora + " para '" + cliente.getNombre() + "'");
            
            if (nueva.getCliente() != null && nueva.getCliente().getCorreo() != null) {
                try {
                    String nombreDoctor = getNombreDoctor(doctor);
                    
                    emailService.enviarRecordatorio(
                        nueva.getCliente().getCorreo(),
                        "✅ Confirmación de cita",
                        "Hola " + nueva.getCliente().getNombre() + ",\n\n" +
                        "Tu cita ha sido AGENDADA.\n\n" +
                        "📅 Fecha: " + nueva.getFecha() + "\n" +
                        "⏰ Hora: " + nueva.getHora() + "\n" +
                        "💼 Servicio: " + nueva.getServicio() + "\n" +
                        "👨‍⚕️ Doctor: " + nombreDoctor + "\n" +
                        "💰 Precio: $" + nueva.getPrecio() + "\n\nGracias."
                    );
                    
                    if (nueva.getAcompananteCorreo() != null && !nueva.getAcompananteCorreo().isEmpty()) {
                        emailService.enviarRecordatorio(
                            nueva.getAcompananteCorreo(),
                            "✅ Confirmación de cita - Acompañante",
                            "Hola " + nueva.getAcompananteNombre() + ",\n\n" +
                            "Has sido registrado como acompañante en una cita.\n\n" +
                            "📅 Fecha: " + nueva.getFecha() + "\n" +
                            "⏰ Hora: " + nueva.getHora() + "\n" +
                            "💼 Servicio: " + nueva.getServicio() + "\n" +
                            "👨‍⚕️ Doctor: " + nombreDoctor + "\n\n" +
                            "Paciente: " + nueva.getCliente().getNombre() + "\n" +
                            "Relación: " + nueva.getAcompananteRelacion() + "\n\nGracias."
                        );
                    }
                } catch (Exception e) {
                    System.err.println("Error enviando correo: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(nueva);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error al crear cita: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reagendar")
    public ResponseEntity<?> reagendar(@PathVariable Long id,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nuevaFecha,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nuevaHora,
                                        @RequestParam(required = false) String motivo) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            Cita cita = citaService.obtenerPorId(id).orElse(null);
            
            if (cita == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (usuario != null && "PACIENTE".equals(usuario.getRol())) {
                Cliente cliente = clienteRepository.findByCorreo(usuario.getUsername());
                if (cliente == null || !cita.getCliente().getId().equals(cliente.getId())) {
                    return ResponseEntity.status(403).body("No puedes reagendar citas de otros pacientes");
                }
            }
            
            // Validar que el especialista asignado esté libre en el nuevo horario
            if (cita.getDoctor() != null && !citaService.verificarDisponibilidadDoctor(cita.getDoctor().getId(), nuevaFecha, nuevaHora)) {
                return ResponseEntity.badRequest().body("El especialista no está disponible en ese horario. Elige otra hora.");
            }
            
            String motivoFinal = (motivo != null && !motivo.isEmpty()) ? motivo : "Cambio de horario solicitado por el paciente";
            Cita citaReagendada = citaService.reagendar(id, nuevaFecha, nuevaHora, motivoFinal);
            
            if (citaReagendada.getCliente() != null && citaReagendada.getCliente().getCorreo() != null) {
                try {
                    String nombreDoctor = getNombreDoctor(citaReagendada.getDoctor());
                    
                    emailService.enviarRecordatorio(
                        citaReagendada.getCliente().getCorreo(),
                        "🔄 Cita reagendada",
                        "Hola " + citaReagendada.getCliente().getNombre() + ",\n\n" +
                        "Tu cita ha sido REAGENDADA.\n\n" +
                        "📅 Fecha original: " + citaReagendada.getFechaOriginal() + "\n" +
                        "⏰ Hora original: " + citaReagendada.getHoraOriginal() + "\n\n" +
                        "📅 Nueva fecha: " + nuevaFecha + "\n" +
                        "⏰ Nueva hora: " + nuevaHora + "\n" +
                        "💼 Servicio: " + citaReagendada.getServicio() + "\n" +
                        "👨‍⚕️ Doctor: " + nombreDoctor + "\n" +
                        "📝 Motivo: " + motivoFinal + "\n\n" +
                        "Si no solicitaste este cambio, contacta con el consultorio."
                    );
                } catch (Exception e) {
                    System.err.println("Error enviando correo: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(citaReagendada);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error al reagendar: " + e.getMessage());
        }
    }
    
    // ==================== ACTUALIZAR ESTADO (DOCTOR PUEDE USARLO) ====================
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestParam String estado) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            
            // Verificar que el usuario existe
            if (usuario == null) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }
            
            // Personal clínico (DOCTOR, PSICOLOGO, PSIQUIATRA, ENFERMERO, TRABAJADOR_SOCIAL)
            // también puede cambiar el estado de sus propias citas
            boolean esClinico = ROLES_CLINICOS.contains(usuario.getRol());
            if (!"ADMIN".equals(usuario.getRol()) && 
                !"EMPLEADO".equals(usuario.getRol()) && 
                !esClinico) {
                return ResponseEntity.status(403).body("No tienes permiso para cambiar el estado de las citas");
            }
            
            // Obtener la cita
            Cita cita = citaService.obtenerPorId(id).orElse(null);
            if (cita == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Si es personal clínico, verificar que la cita le pertenece
            if (esClinico) {
                if (cita.getDoctor() == null || !cita.getDoctor().getId().equals(usuario.getId())) {
                    return ResponseEntity.status(403).body("No puedes cambiar el estado de citas asignadas a otro especialista");
                }
            }
            
            System.out.println("🔵 Cambiando estado de cita " + id + " de " + cita.getEstado() + " a " + estado);
            System.out.println("👤 Usuario: " + usuario.getUsername() + " - Rol: " + usuario.getRol());
            
            Cita citaActualizada = citaService.actualizarEstado(id, estado);
            
            auditoriaService.registrar(usuario.getUsername(),
                "CANCELADA".equals(estado) ? "CANCELACION" : "MODIFICACION",
                "Cita ID " + id + " (" + cita.getServicio() + " del " + cita.getFecha()
                + ") cambia de " + cita.getEstado() + " a " + estado);
            
            return ResponseEntity.ok(citaActualizada);
            
        } catch (Exception e) {
            System.err.println("❌ Error al actualizar estado: " + e.getMessage());
            return ResponseEntity.badRequest().body("❌ Error al actualizar estado: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            Cita cita = citaService.obtenerPorId(id).orElse(null);
            
            if (cita == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (citaService.esCitaPasada(cita)) {
                return ResponseEntity.badRequest().body("No se puede cancelar una cita que ya pasó");
            }
            
            if (usuario != null && "PACIENTE".equals(usuario.getRol())) {
                Cliente cliente = clienteRepository.findByCorreo(usuario.getUsername());
                if (cliente != null && !cita.getCliente().getId().equals(cliente.getId())) {
                    return ResponseEntity.status(403).body("No puedes cancelar citas de otros pacientes");
                }
            }
            
            Cita citaCancelada = citaService.cancelar(id);
            
            if (usuario != null) {
                auditoriaService.registrar(usuario.getUsername(), "CANCELACION",
                    "Cancelación de cita ID " + id + " (" + cita.getServicio() + " del "
                    + cita.getFecha() + ") del paciente '"
                    + (cita.getCliente() != null ? cita.getCliente().getNombre() : "N/A") + "'");
            }
            
            if (citaCancelada.getCliente() != null && citaCancelada.getCliente().getCorreo() != null) {
                try {
                    emailService.enviarRecordatorio(
                        citaCancelada.getCliente().getCorreo(),
                        "❌ Cita cancelada",
                        "Hola " + citaCancelada.getCliente().getNombre() + ",\n\n" +
                        "Tu cita ha sido CANCELADA.\n\n" +
                        "📅 Fecha: " + citaCancelada.getFecha() + "\n" +
                        "⏰ Hora: " + citaCancelada.getHora() + "\n" +
                        "💼 Servicio: " + citaCancelada.getServicio() + "\n" +
                        "Si no solicitaste esta cancelación, contacta con el consultorio."
                    );
                } catch (Exception e) {
                    System.err.println("Error enviando correo: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(citaCancelada);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==================== ENDPOINT DE ASISTENCIA ====================
    @PutMapping("/{id}/asistencia")
    public ResponseEntity<?> marcarAsistencia(@PathVariable Long id, @RequestParam boolean asistio) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            
            if (usuario == null || (!"ADMIN".equals(usuario.getRol()) && !"EMPLEADO".equals(usuario.getRol()))) {
                return ResponseEntity.status(403).body("No tienes permiso para marcar asistencia");
            }
            
            Cita cita = citaService.obtenerPorId(id).orElse(null);
            if (cita == null) {
                return ResponseEntity.notFound().build();
            }
            
            String estadoAsistencia = asistio ? "PRESENTE" : "AUSENTE";
            cita.setAsistencia(estadoAsistencia);
            citaService.actualizar(cita);
            
            // Si el paciente no asistió, cancelar la cita
            if (!asistio && "AGENDADA".equals(cita.getEstado())) {
                cita.setEstado("CANCELADA");
                citaService.actualizar(cita);
                
                if (cita.getCliente() != null && cita.getCliente().getCorreo() != null) {
                    emailService.enviarRecordatorio(
                        cita.getCliente().getCorreo(),
                        "❌ Inasistencia registrada",
                        "Hola " + cita.getCliente().getNombre() + ",\n\n" +
                        "Se ha registrado tu inasistencia a la cita del día " + cita.getFecha() + " a las " + cita.getHora() + ".\n\n" +
                        "Por favor, contacta con el consultorio para reagendar tu cita.\n\nSaludos."
                    );
                }
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", asistio ? "Asistencia marcada como PRESENTE" : "Asistencia marcada como AUSENTE");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al marcar asistencia: " + e.getMessage());
        }
    }

    // ============================================================
    // HORARIOS DISPONIBLES: ahora por servicio (agenda del especialista que lo atendería)
    // ============================================================
    @GetMapping("/horarios-disponibles")
    public ResponseEntity<?> getHorariosDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String servicio) {
        
        List<String> horariosOcupadosFinal;
        
        if (servicio != null && !servicio.isEmpty()) {
            // Disponibilidad real: agenda del especialista que atendería ese servicio
            Set<String> ocupadosSet = new java.util.HashSet<>();
            for (String h : HORARIOS_DISPONIBLES) {
                LocalTime hora = LocalTime.parse(h);
                Usuario doctor = asignarDoctorPorEspecialidad(servicio, fecha, hora);
                if (doctor == null || !citaService.verificarDisponibilidadDoctor(doctor.getId(), fecha, hora)) {
                    ocupadosSet.add(h);
                }
            }
            horariosOcupadosFinal = new ArrayList<>(ocupadosSet);
        } else {
            // Compatibilidad sin servicio: usa el pool de especialistas
            horariosOcupadosFinal = citaService.getHorariosOcupados(fecha);
        }
        final List<String> horariosOcupados = horariosOcupadosFinal;
        
        List<String> horariosDisponibles = HORARIOS_DISPONIBLES.stream()
                .filter(h -> !horariosOcupados.contains(h))
                .toList();
        
        // No mostrar horarios que ya pasaron el día de hoy
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        if (fecha.equals(hoy)) {
            final LocalTime ahoraFinal = ahora;
            horariosDisponibles = horariosDisponibles.stream()
                    .filter(h -> LocalTime.parse(h).isAfter(ahoraFinal))
                    .toList();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("fecha", fecha);
        response.put("horariosDisponibles", horariosDisponibles);
        response.put("horariosOcupados", horariosOcupados);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora) {
        // Ahora es por doctor: si hay ALGÚN especialista libre a esa hora, hay disponibilidad
        boolean disponible = HORARIOS_DISPONIBLES.contains(hora.format(DateTimeFormatter.ofPattern("HH:mm")))
                && citaService.getEspecialesLibres(fecha, hora) > 0;
        return ResponseEntity.ok(disponible);
    }

    @GetMapping("/por-fecha")
    public ResponseEntity<?> listarPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(citaService.listarPorFecha(fecha));
    }

    @GetMapping("/resumen-financiero")
    public ResponseEntity<?> getResumenFinanciero() {
        try {
            Map<String, Object> resumen = citaService.getResumenFinanciero();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/exportar-pdf")
    public void exportarPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                            @RequestParam(defaultValue = "todas") String tipo,
                            @RequestHeader(value = "Authorization", required = false) String authHeader,
                            HttpServletResponse response) throws Exception {
        
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token no proporcionado");
            return;
        }
        
        List<Cita> citas;
        if ("completadas".equals(tipo)) {
            citas = citaService.listarCitasCompletadasPorRango(inicio, fin);
        } else {
            citas = citaService.listarPorRangoFechas(inicio, fin);
        }
        
        pdfExportService.exportarCitas(citas, response);
    }
}