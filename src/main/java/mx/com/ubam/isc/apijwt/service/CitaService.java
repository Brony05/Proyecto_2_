package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.ListaEspera;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ListaEsperaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CitaService {

    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private ListaEsperaRepository listaEsperaRepository;    @Autowired    private EmailService emailService;
    
    @Autowired    private UsuarioRepository usuarioRepository;

    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }

    public Optional<Cita> obtenerPorId(Long id) {
        return citaRepository.findById(id);
    }

    public Cita actualizar(Cita cita) {
        return citaRepository.save(cita);
    }    // NOTA: verificarDisponibilidad global (por fecha/hora en TODO el hospital) se eliminó:
    // bloqueaba citas aunque los especialistas estuvieran libres. La disponibilidad real
    // se valida por doctor con verificarDisponibilidadDoctor.
    public boolean verificarDisponibilidadDoctor(Long doctorId, LocalDate fecha, LocalTime hora) {
        List<Cita> citasDoctor = citaRepository.findByDoctorIdAndFecha(doctorId, fecha);
        return citasDoctor.stream().noneMatch(c -> c.getHora().equals(hora) && !"CANCELADA".equals(c.getEstado()));
    }
    
    public List<String> getHorariosOcupados(LocalDate fecha) {
        List<String> ocupados = new java.util.ArrayList<>();
        // Un horario está ocupado solo si TODOS los especialistas están ocupados a esa hora;
        // con pool = 1 especialista, coincide con su agenda.
        long poolSize = Math.max(1, largoPoolEspecialistas());
        Map<LocalTime, Long> ocupadasPorHora = citaRepository.findByFecha(fecha).stream()
                .filter(c -> !"CANCELADA".equals(c.getEstado()))
                .collect(Collectors.groupingBy(Cita::getHora, Collectors.counting()));
        ocupadasPorHora.forEach((hora, count) -> {
            if (count >= poolSize) {
                ocupados.add(hora.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        });
        return ocupados;
    }
    
    /** Cuántos especialistas están libres a esa fecha/hora (pool completo: Psirologos, Psiquiatras y DOCTORs) */
    public long getEspecialesLibres(LocalDate fecha, LocalTime hora) {
        long total = 0;
        for (Usuario u : poolEspecialistas()) {
            if (verificarDisponibilidadDoctor(u.getId(), fecha, hora)) {
                total++;
            }
        }
        return total;
    }
    
    private long largoPoolEspecialistas() {
        return Math.max(1, poolEspecialistas().size());
    }
    
    private List<Usuario> poolEspecialistas() {
        try {
            List<Usuario> pool = new java.util.ArrayList<>();
            pool.addAll(usuarioRepository.findByRol("PSIQUIATRA"));
            pool.addAll(usuarioRepository.findByRol("PSICOLOGO"));
            pool.addAll(usuarioRepository.findByRol("DOCTOR"));
            return pool;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * La Consulta Inicial se hace UNA sola vez: si el paciente ya tiene una
     * (activa o completada) ya NO puede agendar otra. Solo puede reintentarla
     * si la misma cita fue CANCELADA.
     */
    public boolean yaTieneConsultaInicial(Long clienteId) {
        List<Cita> citas = citaRepository.findByClienteId(clienteId);
        return citas.stream()
                .anyMatch(c -> "Consulta Inicial".equals(c.getServicio()) 
                        && !"CANCELADA".equals(c.getEstado()));
    }
    
    /**
     * Validación suave para reagendar: si ya hizo una Consulta Inicial (activa o completada)
     * NO debe poder "reagendarla" a otro servicio para saltarse el bloqueo.
     */
    public boolean tuvoConsultasIniciales(Long clienteId) {
        List<Cita> citas = citaRepository.findByClienteId(clienteId);
        return citas.stream()
                .anyMatch(c -> "Consulta Inicial".equals(c.getServicio())
                        && !"CANCELADA".equals(c.getEstado()));
    }

    public boolean yaTieneCitaEnFecha(Long clienteId, LocalDate fecha) {
        List<Cita> citas = citaRepository.findByClienteId(clienteId);
        return citas.stream()
                .anyMatch(c -> c.getFecha().equals(fecha) 
                        && !"CANCELADA".equals(c.getEstado()));
    }

    public Cita crear(Cita cita) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        
        if (cita.getFecha().isBefore(hoy)) {
            throw new RuntimeException("No se pueden agendar citas en fechas pasadas");
        }
        
        if (cita.getFecha().equals(hoy) && cita.getHora().isBefore(ahora)) {
            throw new RuntimeException("No se puede agendar una cita para una hora que ya pasó. La hora actual es: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        
        LocalDate maxFecha = hoy.plusDays(30);
        if (cita.getFecha().isAfter(maxFecha)) {
            throw new RuntimeException("Solo se pueden agendar citas con máximo 30 días de anticipación");
        }
        
        if (cita.getHora().isBefore(LocalTime.of(9, 0)) || cita.getHora().isAfter(LocalTime.of(18, 0))) {
            throw new RuntimeException("El horario de atención es de 9:00 a 18:00");
        }
        
        int minutos = cita.getHora().getMinute();
        if (minutos != 0 && minutos != 30) {
            throw new RuntimeException("Las citas solo se pueden agendar en punto o media hora (ej: 9:00, 9:30)");
        }
        
        // Ya NO se valida disponibilidad global: cada doctor tiene su propia agenda
        // y la verificación por doctor se hace antes de llamar a este método.
        
        if (cita.getCliente() != null && yaTieneCitaEnFecha(cita.getCliente().getId(), cita.getFecha())) {
            throw new RuntimeException("Ya tienes una cita agendada para esta fecha. Solo puedes tener una cita por día.");
        }
        
        if ("Consulta Inicial".equals(cita.getServicio()) && cita.getCliente() != null) {
            if (yaTieneConsultaInicial(cita.getCliente().getId())) {
                throw new RuntimeException("Ya has agendado una Consulta Inicial anteriormente. Solo puedes tener una Consulta Inicial.");
            }
        }
        
        if (cita.getEstado() == null || cita.getEstado().isEmpty()) {
            cita.setEstado("AGENDADA");
        }
        
        if (cita.getServicio() == null || cita.getServicio().isEmpty()) {
            cita.setServicio("Consulta General");
        }
        
        if (cita.getPrecio() == null) {
            cita.setPrecio(500.0);
        }
        
        if (cita.getAsistencia() == null) {
            cita.setAsistencia("PENDIENTE");
        }
        
        return citaRepository.save(cita);
    }

    public Cita cancelar(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        
        if (cita.getFecha().isBefore(hoy) || (cita.getFecha().equals(hoy) && cita.getHora().isBefore(ahora))) {
            throw new RuntimeException("No se puede cancelar una cita que ya pasó");
        }
        
        String servicioOriginal = cita.getServicio();
        LocalDate fechaOriginal = cita.getFecha();
        LocalTime horaOriginal = cita.getHora();
        
        cita.setEstado("CANCELADA");
        Cita citaCancelada = citaRepository.save(cita);
        
        notificarListaEspera(fechaOriginal, horaOriginal, servicioOriginal);
        
        return citaCancelada;
    }
    
    public Cita reagendar(Long id, LocalDate nuevaFecha, LocalTime nuevaHora, String motivo) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        
        if ("CANCELADA".equals(cita.getEstado())) {
            throw new RuntimeException("No se puede reagendar una cita cancelada");
        }
        
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        
        if (cita.getFecha().isBefore(hoy) || (cita.getFecha().equals(hoy) && cita.getHora().isBefore(ahora))) {
            throw new RuntimeException("No se puede reagendar una cita que ya pasó");
        }
        
        if (nuevaFecha.isBefore(hoy)) {
            throw new RuntimeException("No se pueden reagendar citas en fechas pasadas");
        }
        
        if (nuevaFecha.equals(hoy) && nuevaHora.isBefore(ahora)) {
            throw new RuntimeException("No se puede reagendar a una hora que ya pasó. La hora actual es: " + ahora.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        
        if (nuevaHora.isBefore(LocalTime.of(9, 0)) || nuevaHora.isAfter(LocalTime.of(18, 0))) {
            throw new RuntimeException("El horario de atención es de 9:00 a 18:00");
        }
        
        int minutos = nuevaHora.getMinute();
        if (minutos != 0 && minutos != 30) {
            throw new RuntimeException("Las citas solo se pueden agendar en punto o media hora");
        }
        
        if (cita.getCliente() != null && yaTieneCitaEnFecha(cita.getCliente().getId(), nuevaFecha)) {
            if (!cita.getFecha().equals(nuevaFecha)) {
                throw new RuntimeException("Ya tienes una cita agendada para esta fecha. Solo puedes tener una cita por día.");
            }
        }
        
        // Protección extra: la cita de Consulta Inicial solo se mueve si no hay OTRAS consultas iniciales activas
        if (cita.getCliente() != null && "Consulta Inicial".equals(cita.getServicio()) 
                && yaTieneConsultaInicial(cita.getCliente().getId())) {
            throw new RuntimeException("Ya existe otra Consulta Inicial activa para este paciente.");
        }
        
        LocalDate fechaOriginal = cita.getFecha();
        LocalTime horaOriginal = cita.getHora();
        String servicioOriginal = cita.getServicio();
        
        cita.setFechaOriginal(cita.getFecha());
        cita.setHoraOriginal(cita.getHora());
        cita.setMotivoReagendado(motivo);
        cita.setFecha(nuevaFecha);
        cita.setHora(nuevaHora);
        cita.setEstado("REAGENDADA");
        
        Cita citaReagendada = citaRepository.save(cita);
        
        notificarListaEspera(fechaOriginal, horaOriginal, servicioOriginal);
        
        return citaReagendada;
    }
    
    public void notificarListaEspera(LocalDate fecha, LocalTime hora, String servicio) {
        try {
            List<ListaEspera> interesados = listaEsperaRepository.findByServicioInteresAndNotificadoFalse(servicio);
            for (ListaEspera interesado : interesados) {
                try {
                    Cliente paciente = interesado.getPaciente();
                    if (paciente != null && paciente.getCorreo() != null && !paciente.getCorreo().isEmpty()) {
                        emailService.enviarRecordatorio(
                            paciente.getCorreo(),
                            "🔔 Horario disponible - Consultorio Psiquiátrico",
                            "Hola " + paciente.getNombre() + ",\n\n" +
                            "Se ha liberado un horario para el servicio: " + servicio + "\n\n" +
                            "📅 Fecha disponible: " + fecha + "\n" +
                            "⏰ Hora disponible: " + hora + "\n\n" +
                            "Si aún estás interesado, ingresa al sistema y agenda tu cita cuanto antes.\n" +
                            "Saludos cordiales,\nConsultorio Psiquiátrico"
                        );
                        interesado.setNotificado(true);
                        listaEsperaRepository.save(interesado);
                    }
                } catch (Exception e) {
                    System.err.println("Error notificando a " + interesado.getPaciente().getCorreo() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al procesar lista de espera: " + e.getMessage());
        }
    }
    
    public Cita actualizarEstado(Long id, String nuevoEstado) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        
        String estadoActual = cita.getEstado();
        
        System.out.println("🔵 actualizarEstado - Estado actual: " + estadoActual + " -> Nuevo: " + nuevoEstado);
        
        // Transiciones permitidas
        if ("AGENDADA".equals(estadoActual) && "EN_PROGRESO".equals(nuevoEstado)) {
            cita.setEstado(nuevoEstado);
        } 
        else if ("EN_PROGRESO".equals(estadoActual) && "COMPLETADA".equals(nuevoEstado)) {
            cita.setEstado(nuevoEstado);
        }
        else if ("AGENDADA".equals(estadoActual) && "CANCELADA".equals(nuevoEstado)) {
            cita.setEstado(nuevoEstado);
        }
        else if ("REAGENDADA".equals(estadoActual) && "AGENDADA".equals(nuevoEstado)) {
            cita.setEstado(nuevoEstado);
        }
        else if ("AGENDADA".equals(estadoActual) && "COMPLETADA".equals(nuevoEstado)) {
            cita.setEstado(nuevoEstado);
        }
        else if ("REAGENDADA".equals(estadoActual) && "COMPLETADA".equals(nuevoEstado)) {
            cita.setEstado(nuevoEstado);
        }
        else {
            throw new RuntimeException("Transición de estado no válida: " + estadoActual + " → " + nuevoEstado);
        }
        
        return citaRepository.save(cita);
    }
    
    public boolean esCitaPasada(Cita cita) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        
        if (cita.getFecha().isBefore(hoy)) {
            return true;
        }
        if (cita.getFecha().equals(hoy) && cita.getHora().isBefore(ahora)) {
            return true;
        }
        return false;
    }

    public List<Cita> listarPorFecha(LocalDate fecha) {
        return citaRepository.findByFecha(fecha);
    }

    public List<Cita> listarPorRangoFechas(LocalDate inicio, LocalDate fin) {
        return citaRepository.findByFechaBetween(inicio, fin);
    }
    
    public List<Cita> listarPorCliente(Long clienteId) {
        if (clienteId == null) {
            return List.of();
        }
        return citaRepository.findByClienteId(clienteId);
    }
    
    public List<Cita> listarCitasCompletadas() {
        return citaRepository.findByEstado("COMPLETADA");
    }

    public List<Cita> listarCitasCompletadasPorRango(LocalDate inicio, LocalDate fin) {
        return citaRepository.findByEstadoAndFechaBetween("COMPLETADA", inicio, fin);
    }

    public List<Cita> listarCitasAgendadas() {
        List<Cita> agendadas = citaRepository.findByEstado("AGENDADA");
        List<Cita> reagendadas = citaRepository.findByEstado("REAGENDADA");
        agendadas.addAll(reagendadas);
        return agendadas;
    }

    public double calcularIngresosReales() {
        return listarCitasCompletadas().stream()
                .mapToDouble(c -> c.getPrecio() != null ? c.getPrecio() : 0)
                .sum();
    }

    public double calcularIngresosProyectados() {
        return listarCitasAgendadas().stream()
                .mapToDouble(c -> c.getPrecio() != null ? c.getPrecio() : 0)
                .sum();
    }

    public double calcularPerdidasPorCancelacion() {
        return citaRepository.findByEstado("CANCELADA").stream()
                .mapToDouble(c -> c.getPrecio() != null ? c.getPrecio() : 0)
                .sum();
    }

    public Map<String, Object> getResumenFinanciero() {
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("ingresosReales", calcularIngresosReales());
        resumen.put("ingresosProyectados", calcularIngresosProyectados());
        resumen.put("perdidasCancelacion", calcularPerdidasPorCancelacion());
        resumen.put("totalCitasCompletadas", listarCitasCompletadas().size());
        resumen.put("totalCitasAgendadas", listarCitasAgendadas().size());
        resumen.put("totalCitasCanceladas", citaRepository.findByEstado("CANCELADA").size());
        return resumen;
    }
    
    public List<Cita> listarCitasPorDoctor(Long doctorId) {
        return citaRepository.findByDoctorId(doctorId);
    }
    
    public List<Cita> listarCitasPorDoctorYFecha(Long doctorId, LocalDate fecha) {
        return citaRepository.findByDoctorIdAndFecha(doctorId, fecha);
    }
}