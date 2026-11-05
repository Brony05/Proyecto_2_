package mx.com.ubam.isc.apijwt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import mx.com.ubam.isc.apijwt.model.Asignacion;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.AsignacionRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Sembrado de ASIGNACIONES paciente–profesional para la demo.
 *
 * Corre después de {@link SeedDatosRealistas}, cuando ya existen pacientes y
 * especialistas. Reparte pacientes entre enfermería, psicología, psiquiatría y
 * trabajo social para que las vistas de esos roles tengan datos reales desde
 * el primer arranque (antes dependían de citas históricas del departamento).
 *
 * Es IDEMPOTENTE: si ya hay asignaciones registradas no hace nada.
 */
@Component
@Order(3)
public class SeedAsignaciones implements CommandLineRunner {

    /** Cuántos pacientes de demo recibe cada profesional. */
    private static final int PACIENTES_POR_PROFESIONAL = 3;

    @Autowired private AsignacionRepository asignacionRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        if (asignacionRepository.count() > 0) {
            System.out.println("ℹ️ [SEED] Asignaciones ya presentes ("
                + asignacionRepository.count() + "). No se vuelven a sembrar.");
            return;
        }

        List<Cliente> pacientes = clienteRepository.findAll();
        if (pacientes.isEmpty()) {
            return;
        }

        int creadas = 0;
        int indicePaciente = 0;

        creadas += asignarRol("ENFERMERO", "ENFERMERIA", "Control de signos vitales", pacientes, indicePaciente);
        indicePaciente += PACIENTES_POR_PROFESIONAL;
        creadas += asignarRol("PSICOLOGO", "PSICOLOGIA", "Seguimiento psicoterapéutico", pacientes, indicePaciente);
        indicePaciente += PACIENTES_POR_PROFESIONAL;
        creadas += asignarRol("PSIQUIATRA", "PSIQUIATRIA", "Valoración psiquiátrica", pacientes, indicePaciente);
        indicePaciente += PACIENTES_POR_PROFESIONAL;
        creadas += asignarRol("TRABAJADOR_SOCIAL", "TRABAJO_SOCIAL", "Estudio socioeconómico", pacientes, indicePaciente);

        System.out.println("🔗 [SEED] Asignaciones paciente–profesional creadas: " + creadas);
    }

    /**
     * Asigna a cada profesional activo de un rol un bloque de pacientes,
     * empezando en {@code desde} y recorriendo la lista de forma circular.
     */
    private int asignarRol(String rol, String departamento, String motivo,
                           List<Cliente> pacientes, int desde) {
        List<Usuario> profesionales = usuarioRepository.findByRol(rol);
        int creadas = 0;
        int cursor = desde;

        for (Usuario profesional : profesionales) {
            for (int i = 0; i < PACIENTES_POR_PROFESIONAL; i++) {
                Cliente cliente = pacientes.get(cursor % pacientes.size());
                cursor++;

                if (asignacionRepository.existsByUsuarioAndClienteAndActivaTrue(profesional, cliente)) {
                    continue;
                }

                Asignacion asignacion = new Asignacion();
                asignacion.setCliente(cliente);
                asignacion.setUsuario(profesional);
                asignacion.setDepartamento(departamento);
                asignacion.setFechaInicio(LocalDate.now().minusDays(30));
                asignacion.setActiva(true);
                asignacion.setMotivo(motivo);
                asignacionRepository.save(asignacion);
                creadas++;
            }
        }
        return creadas;
    }
}
