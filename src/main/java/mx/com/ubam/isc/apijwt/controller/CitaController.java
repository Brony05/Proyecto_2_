package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Servicio;
import mx.com.ubam.isc.apijwt.service.CitaService;
import mx.com.ubam.isc.apijwt.service.EmailService;
import mx.com.ubam.isc.apijwt.service.PdfExportService;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.ServicioRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ServicioRepository servicioRepository;

    private static final List<String> HORARIOS_DISPONIBLES = List.of(
        "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00", "16:30", "17:00", "17:30", "18:00"
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

    private Usuario asignarDoctorPorEspecialidad(String servicioNombre) {
        String especialidadRequerida;
        switch (servicioNombre) {
            case "Consulta Inicial": especialidadRequerida = "Psicología Clínica"; break;
            case "Terapia Psicológica": especialidadRequerida = "Psicología Clínica"; break;
            case "Seguimiento": especialidadRequerida = "Psicología Clínica"; break;
            case "Urgencia Psiquiátrica": especialidadRequerida = "Psiquiatría"; break;
            case "Valoración": especialidadRequerida = "Neuropsicología"; break;
            case "Terapia de Pareja": especialidadRequerida = "Terapia de Pareja"; break;
            case "Psicología Infantil": especialidadRequerida = "Psicología Infantil"; break;
            case "Neuropsicología": especialidadRequerida = "Neuropsicología"; break;
            case "Psiquiatría": especialidadRequerida = "Psiquiatría"; break;
            default: especialidadRequerida = "Psicología Clínica";
        }
        
        List<Usuario> doctores = usuarioRepository.findByRolAndEspecialidad("DOCTOR", especialidadRequerida);
        if (doctores.isEmpty()) {
            return null;
        }
        return doctores.get(0);
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
            
            Usuario doctor = asignarDoctorPorEspecialidad(servicioNombre);
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
            
            if (!citaService.verificarDisponibilidad(fecha, hora)) {
                return ResponseEntity.badRequest().body("El horario " + hora + " ya está ocupado.");
            }
            
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
            
            // DOCTOR también puede cambiar estado
            if (!"ADMIN".equals(usuario.getRol()) && 
                !"EMPLEADO".equals(usuario.getRol()) && 
                !"DOCTOR".equals(usuario.getRol())) {
                return ResponseEntity.status(403).body("No tienes permiso para cambiar el estado de las citas");
            }
            
            // Obtener la cita
            Cita cita = citaService.obtenerPorId(id).orElse(null);
            if (cita == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Si es DOCTOR, verificar que la cita le pertenece
            if ("DOCTOR".equals(usuario.getRol())) {
                if (cita.getDoctor() == null || !cita.getDoctor().getId().equals(usuario.getId())) {
                    return ResponseEntity.status(403).body("No puedes cambiar el estado de citas de otro doctor");
                }
            }
            
            System.out.println("🔵 Cambiando estado de cita " + id + " de " + cita.getEstado() + " a " + estado);
            System.out.println("👤 Usuario: " + usuario.getUsername() + " - Rol: " + usuario.getRol());
            
            Cita citaActualizada = citaService.actualizarEstado(id, estado);
            
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

    @GetMapping("/horarios-disponibles")
    public ResponseEntity<?> getHorariosDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        
        List<String> horariosOcupados = citaService.getHorariosOcupados(fecha);
        List<String> horariosDisponibles = HORARIOS_DISPONIBLES.stream()
                .filter(h -> !horariosOcupados.contains(h))
                .toList();
        
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
        return ResponseEntity.ok(citaService.verificarDisponibilidad(fecha, hora));
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