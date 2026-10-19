package mx.com.ubam.isc.apijwt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.Expediente;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ExpedienteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Sembrado de datos REALISTAS para probar el sistema con volumen:
 *  - Llega a 50 usuarios PACIENTE en total (usa los 3 existentes como base).
 *  - Agrega especialistas clínicos nuevos para repartir la carga horaria.
 *  - Genera historial de citas desde ENERO 2026 hasta HOY, incluyendo:
 *      * Consulta Inicial para cada paciente (40 completadas).
 *      * Seguimientos mensuales ~75% COMPLETADAS (80% PRESENTE / 20% AUSENTE),
 *        ~15% CANCELADAS y ~10% REAGENDADAS (con fecha/motivo originales).
 *      * Citas de HOY (algunas COMPLETADAS, algunas EN_PROGRESO).
 *      * Citas FUTURAS AGENDADAS para esta semana.
 *  - Es IDEMPOTENTE: si ya hay 50 pacientes, no hace nada en el arranque.
 */
@Component
@Order(2) // corre después de DataLoader (crea los 3 pacientes base + servicios)
public class SeedDatosRealistas implements CommandLineRunner {

    private static final int TOTAL_PACIENTES = 50;
    private static final String PASSWORD = "123456";

    private static final String[] HORAS = {
        "09:00","09:30","10:00","10:30","11:00","11:30","12:00","12:30",
        "13:00","13:30","14:00","14:30","15:00","15:30","16:00","16:30",
        "17:00","17:30","18:00"
    };

    private static final String[] MOTIVOS_REAGENDA = {
        "Cambio de horario solicitado por el paciente",
        "Conflicto de agenda del especialista",
        "Paciente solicitó otra franja por trabajo",
        "Reprogramación por indicación médica",
        "El hospital ajustó los horarios de esa jornada"
    };

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private ExpedienteRepository expedienteRepository;

    @Override
    public void run(String... args) {
        List<Usuario> pacientesExistentes = usuarioRepository.findByRol("PACIENTE");
        if (pacientesExistentes.size() >= TOTAL_PACIENTES) {
            System.out.println("ℹ️ [SEED] Datos realistas ya presentes ("
                + pacientesExistentes.size() + " pacientes). No se vuelve a sembrar.");
            return;
        }
        System.out.println("🌱 [SEED] Sembrando datos realistas (" + pacientesExistentes.size()
            + " pacientes actuales → objetivo " + TOTAL_PACIENTES + ")...");

        Random rnd = new Random(20260101L); // determinista: la DEMO se ve igual en cada regeneración

        // ============================================================
        // 1. Especialistas clínicos NUEVOS (para repartir agenda)
        // ============================================================
        String[][] clinicosNuevos = {
            {"psicologo4", "PSICOLOGO", "Lic. Alberto Sandoval", "PSICOLOGIA", "Psicología Clínica"},
            {"psicologo5", "PSICOLOGO", "Lic. Norma Escalante", "PSICOLOGIA", "Psicología Infantil"},
            {"psicologo6", "PSICOLOGO", "Lic. Óscar Canedo", "PSICOLOGIA", "Terapia de Pareja"},
            {"psiquiatra4", "PSIQUIATRA", "Dr. Jorge Santillán", "PSIQUIATRIA", "Psiquiatría"},
            {"psiquiatra5", "PSIQUIATRA", "Dra. Beatriz Alvarado", "PSIQUIATRIA", "Neuropsicología"},
            {"karla2", "DOCTOR", "Dra. Karla Montaño", "PSICOLOGIA", "Psicología Clínica"},
            {"enfermero4", "ENFERMERO", "Enf. Diana Cordero", "ENFERMERIA", null},
            {"ts3", "TRABAJADOR_SOCIAL", "Lic. Fernando Anaya", "TRABAJO_SOCIAL", null}
        };
        for (String[] c : clinicosNuevos) {
            crearUsuarioSiNoExiste(c[0], c[1], c[2], c[3], c[4], rnd);
        }

        // ============================================================
        // 2. PACIENTES: completar hasta 50
        // ============================================================
        String[] nombresM = {"Alejandro","Fernando","Diego","Carlos","Andrés","Miguel","Jorge",
            "Ricardo","Daniel","Emilio","Gerardo","Héctor","Iván","Julián","Lorenzo","Manuel",
            "Óscar","Pablo","Raúl","Sergio","Tomás","Víctor","Rubén","Esteban","Marco","Jaime",
            "Adrián","Filiberto","Moisés","Ernesto"};
        String[] nombresF = {"Lucía","Fernanda","Sofía","Valeria","Renata","Daniela","Camila",
            "Ximena","Regina","Elena","Martina","Julia","Isabel","Clara","Gabriela","Mónica",
            "Patricia","Silvia","Andrea","Brenda","Cecilia","Diana","Alejandra","Rocío","Nadia",
            "Gloria","Esperanza","Verónica","Marisol","Paula"};
        String[] apellidos = {"Hernández","García","Martínez","López","González","Pérez","Sánchez",
            "Ramírez","Torres","Flores","Rivera","Gómez","Díaz","Cruz","Morales","Reyes",
            "Gutiérrez","Ortiz","Chávez","Ruiz","Mendoza","Vargas","Castillo","Romero","Herrera",
            "Medina","Aguilar","Vega","Campos","Soto","Delgado","Ríos","Peña","Navarro","Solís"};

        int faltantes = TOTAL_PACIENTES - pacientesExistentes.size();
        List<Cliente> pacientesNuevos = new ArrayList<>();
        for (int i = 0; i < faltantes; i++) {
            int id = pacientesExistentes.size() + 1 + i; // paciente4, paciente5...
            String username = "paciente" + id;
            boolean fem = rnd.nextBoolean();
            String nombre = (fem ? nombresF : nombresM)[rnd.nextInt(nombresM.length)]
                + " " + apellidos[rnd.nextInt(apellidos.length)]
                + " " + apellidos[rnd.nextInt(apellidos.length)];

            UsuariosGuardados guardado = crearPacienteYCliente(username, nombre, rnd);
            if (guardado.cliente != null) {
                pacientesNuevos.add(guardado.cliente);
            }
        }

        // Enlazar también los 3 pacientes de prueba (por si no tienen Cliente aún)
        for (Usuario u : pacientesExistentes) {
            if (clienteRepository.findByCorreo(u.getUsername()) == null) {
                Cliente c = new Cliente();
                c.setNombre(u.getNombreCompleto() != null ? u.getNombreCompleto() : u.getUsername());
                c.setCorreo(u.getUsername());
                c.setTelefono(generarTelefono(rnd));
                clienteRepository.save(c);
            }
        }

        // Expedientes para los nuevos clientes
        for (Cliente c : pacientesNuevos) {
            if (expedienteRepository.findByPacienteId(c.getId()).isEmpty()) {
                Expediente e = new Expediente();
                e.setPaciente(c);
                e.setEstado("ACTIVO");
                e.setObservaciones("Expediente abierto al ingreso del paciente");
                e.setFechaActualizacion(LocalDateTime.now());
                expedienteRepository.save(e);
            }
        }

        // ============================================================
        // 3. HISTORIAL DE CITAS: desde enero 2026 hasta hoy
        // ============================================================
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        // Todos los pacientes (viejitos + nuevos) reciben su historial
        List<Cliente> todosPacientes = new ArrayList<>();
        for (Usuario u : pacientesExistentes) {
            Cliente c = clienteRepository.findByCorreo(u.getUsername());
            if (c != null) todosPacientes.add(c);
        }
        todosPacientes.addAll(pacientesNuevos);

        int totalCitas = 0;
        for (int idx = 0; idx < todosPacientes.size(); idx++) {
            totalCitas += sembrarHistorialPaciente(todosPacientes.get(idx), idx, hoy, inicio, rnd);
        }

        System.out.println("========================================");
        System.out.println("✅ [SEED] DATOS REALISTAS CARGADOS");
        System.out.println("   Pacientes totales: " + usuarioRepository.findByRol("PACIENTE").size());
        System.out.println("   Citas generadas:   " + totalCitas);
        System.out.println("   (próximo arranque: se omite el sembrado)");
        System.out.println("========================================");
    }

    // ============================================================
    // Historial de UN paciente
    // ============================================================
    private int sembrarHistorialPaciente(Cliente paciente, int idx, LocalDate hoy, LocalDate inicio, Random rnd) {
        List<Usuario> especialistas = poolClinicos();
        if (especialistas.isEmpty()) return 0;

        int creadas = 0;
        // Un 10% de pacientes deja de acudir tras unos meses (historia realista)
        int mesUltimaCita = (idx % 10 == 0) ? 4 + rnd.nextInt(3) : 12;

        // ---- Consulta Inicial en enero 2026 ----
        LocalDate fechaCI = inicio.plusDays(10 + rnd.nextInt(15));
        Usuario doctorCI = especialistas.get(rnd.nextInt(especialistas.size()));
        boolean noAcudio = rnd.nextInt(100) < 12;
        if (noAcudio) {
            crearCita(paciente, doctorCI, "Consulta Inicial", 450.0, fechaCI, "10:00", "CANCELADA", "AUSENTE");
        } else {
            crearCita(paciente, doctorCI, "Consulta Inicial", 450.0, fechaCI,
                "10:00", "COMPLETADA", rnd.nextInt(100) < 85 ? "PRESENTE" : "AUSENTE");
        }
        creadas++;

        // ---- Seguimientos mensuales feb-octubre ----
        Object[][] seguimientos = {
            {"Terapia Psicológica Individual", 400.0},
            {"Terapia de Seguimiento", 350.0},
            {"Valoración Psicológica", 450.0},
            {"Terapia de Pareja", 500.0},
            {"Terapia Familiar", 500.0},
            {"Terapia Infantil y del Adolescente", 450.0},
            {"Valoración Psiquiátrica", 600.0},
            {"Consulta de Seguimiento Psiquiátrico", 300.0},
            {"Valoración Neuropsicológica", 900.0}
        };

        // Día fijo por paciente para evitar colisiones (2 veces por mes)
        for (int mes = 2; mes <= hoy.getMonthValue(); mes++) {
            if (mes > mesUltimaCita) break;
            int citasEsteMes = 1 + rnd.nextInt(2); // 1 o 2 citas por mes
            for (int k = 0; k < citasEsteMes; k++) {
                Object[] serv = seguimientos[rnd.nextInt(seguimientos.length)];
                String nombreServ = (String) serv[0];
                Double precioServ = (Double) serv[1];
                Usuario doctor = especialistas.get(rnd.nextInt(especialistas.size()));
                LocalDate fecha = LocalDate.of(2026, mes, 2 + rnd.nextInt(26));
                if (!fecha.isBefore(hoy)) continue; // seguridad: histórico puro

                int dado = rnd.nextInt(100);
                if (dado < 15) {
                    crearCita(paciente, doctor, nombreServ, precioServ, fecha, horaAleatoria(rnd), "CANCELADA", "AUSENTE");
                } else if (dado < 25) {
                    LocalDate fut = fecha.plusDays(3 + rnd.nextInt(20));
                    crearCitaReagendada(paciente, doctor, nombreServ, precioServ, fut, horaAleatoria(rnd),
                        fecha, MOTIVOS_REAGENDA[rnd.nextInt(MOTIVOS_REAGENDA.length)]);
                } else {
                    crearCita(paciente, doctor, nombreServ, precioServ, fecha, horaAleatoria(rnd),
                        "COMPLETADA", rnd.nextInt(100) < 80 ? "PRESENTE" : "AUSENTE");
                }
                creadas++;
            }
        }

        // ---- HOY: citas en curso / completadas recientemente ----
        if (idx % 2 == 0) {
            Object[] serv = seguimientos[rnd.nextInt(seguimientos.length)];
            Usuario doctor = especialistas.get(idx % especialistas.size());
            int dado = rnd.nextInt(100);
            if (dado < 30) {
                crearCita(paciente, doctor, (String) serv[0], (Double) serv[1], hoy, horaAleatoria(rnd), "EN_PROGRESO", "PENDIENTE");
            } else if (dado < 40) {
                crearCita(paciente, doctor, (String) serv[0], (Double) serv[1], hoy, horaAleatoria(rnd), "CANCELADA", "AUSENTE");
            } else {
                crearCita(paciente, doctor, (String) serv[0], (Double) serv[1], hoy, horaAleatoria(rnd), "COMPLETADA", "PRESENTE");
            }
            creadas++;
        }

        // ---- Futuras AGENDADAS para esta semana ----
        int citasFuturas = 1 + rnd.nextInt(3); // 1-3 próximas
        for (int j = 0; j < citasFuturas; j++) {
            Object[] serv = seguimientos[rnd.nextInt(seguimientos.length)];
            Usuario doctor = especialistas.get((idx + j) % especialistas.size());
            LocalDate fut = hoy.plusDays(1 + rnd.nextInt(7));
            if (rnd.nextInt(100) < 8) {
                // Algunas se muestran como REAGENDADAS pendientes de acudir
                LocalDate orig = hoy.minusDays(5 + rnd.nextInt(20));
                crearCitaReagendada(paciente, doctor, (String) serv[0], (Double) serv[1], fut, horaAleatoria(rnd),
                    orig, MOTIVOS_REAGENDA[rnd.nextInt(MOTIVOS_REAGENDA.length)]);
            } else {
                crearCita(paciente, doctor, (String) serv[0], (Double) serv[1], fut, horaAleatoria(rnd), "AGENDADA", "PENDIENTE");
            }
            creadas++;
        }

        return creadas;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private List<Usuario> poolClinicos() {
        List<Usuario> pool = new ArrayList<>();
        for (String rol : new String[]{"DOCTOR", "PSICOLOGO", "PSIQUIATRA"}) {
            pool.addAll(usuarioRepository.findByRol(rol));
        }
        return pool;
    }

    private void crearCita(Cliente cliente, Usuario doctor, String servicio, Double precio,
                           LocalDate fecha, String horaStr, String estado, String asistencia) {
        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setDoctor(doctor);
        cita.setUsuario(doctor);
        cita.setServicio(servicio);
        cita.setPrecio(precio);
        cita.setFecha(fecha);
        cita.setHora(LocalTime.parse(horaStr));
        cita.setEstado(estado);
        cita.setAsistencia(asistencia);
        citaRepository.save(cita);
    }

    private void crearCitaReagendada(Cliente cliente, Usuario doctor, String servicio, Double precio,
                                     LocalDate nuevaFecha, String nuevaHora,
                                     LocalDate fechaOriginal, String motivo) {
        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setDoctor(doctor);
        cita.setUsuario(doctor);
        cita.setServicio(servicio);
        cita.setPrecio(precio);
        cita.setFecha(nuevaFecha);
        cita.setHora(LocalTime.parse(nuevaHora));
        cita.setEstado("REAGENDADA");
        cita.setAsistencia("PENDIENTE");
        cita.setFechaOriginal(fechaOriginal);
        cita.setHoraOriginal(LocalTime.parse("10:00"));
        cita.setMotivoReagendado(motivo);
        citaRepository.save(cita);
    }

    private String horaAleatoria(Random rnd) {
        return HORAS[rnd.nextInt(HORAS.length)];
    }

    private String generarTelefono(Random rnd) {
        return String.format("55%08d", rnd.nextInt(100000000));
    }

    private void crearUsuarioSiNoExiste(String username, String rol, String nombre,
                                        String departamento, String especialidad, Random rnd) {
        if (usuarioRepository.findByUsername(username).isPresent()) return;
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRol(rol);
        u.setNombreCompleto(nombre);
        if (departamento != null) u.setDepartamento(departamento);
        if (especialidad != null && !especialidad.isEmpty()) u.setEspecialidad(especialidad);
        u.setCorreo(username);
        u.setTelefono(generarTelefono(rnd));
        usuarioRepository.save(u);
        System.out.println("✅ [SEED] Especialista creado: " + username + " (" + rol + ")");
    }

    private static class UsuariosGuardados {
        Cliente cliente;
    }

    private UsuariosGuardados crearPacienteYCliente(String username, String nombreCompleto, Random rnd) {
        UsuariosGuardados out = new UsuariosGuardados();
        if (usuarioRepository.findByUsername(username).isPresent()) return out;

        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRol("PACIENTE");
        u.setNombreCompleto(nombreCompleto);
        usuarioRepository.save(u);

        Cliente cliente = new Cliente();
        cliente.setNombre(nombreCompleto);
        cliente.setCorreo(username);
        cliente.setTelefono(generarTelefono(rnd));
        cliente = clienteRepository.save(cliente);

        out.cliente = cliente;
        System.out.println("✅ [SEED] Paciente creado: " + username + " (" + nombreCompleto + ")");
        return out;
    }
}
