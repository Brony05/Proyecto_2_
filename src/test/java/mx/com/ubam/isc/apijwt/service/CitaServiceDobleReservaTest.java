package mx.com.ubam.isc.apijwt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ListaEsperaRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;

/**
 * Regresión de SC-03: un especialista no puede tener dos citas activas en el
 * mismo horario, y reagendar no puede mover una cita a un horario ocupado.
 *
 * Son pruebas de unidad con repositorios simulados: no tocan la base real, así
 * que se pueden correr en cualquier equipo sin datos clínicos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitaServiceDobleReservaTest {

    @Mock private CitaRepository citaRepository;
    @Mock private ListaEsperaRepository listaEsperaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;

    @InjectMocks private CitaService citaService;

    private final LocalDate manana = LocalDate.now().plusDays(3);

    private Usuario doctor() {
        Usuario doctor = new Usuario();
        doctor.setId(12L);
        doctor.setUsername("Juan");
        doctor.setNombreCompleto("Juan Pérez");
        doctor.setRol("DOCTOR");
        return doctor;
    }

    private Cita cita(LocalDate fecha, LocalTime hora) {
        Cita cita = new Cita();
        cita.setId(500L);
        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setServicio("Seguimiento");
        cita.setDoctor(doctor());
        return cita;
    }

    @Test
    void crearRechazaUnHorarioQueYaTieneCitaActiva() {
        Cita cita = cita(manana, LocalTime.of(9, 0));

        when(usuarioRepository.bloquearPorId(12L)).thenReturn(Optional.of(doctor()));
        when(citaRepository.buscarCupoOcupado(12L, manana, LocalTime.of(9, 0), null))
                .thenReturn(List.of(cita(manana, LocalTime.of(9, 0))));

        RuntimeException error = assertThrows(RuntimeException.class, () -> citaService.crear(cita));

        assertTrue(error.getMessage().contains("ya está ocupado"),
                "El mensaje debe explicar que el horario está ocupado, fue: " + error.getMessage());
        assertTrue(error.getMessage().contains("Juan Pérez"),
                "El mensaje debe nombrar al especialista, fue: " + error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void crearGuardaCuandoElHorarioEstaLibre() {
        Cita cita = cita(manana, LocalTime.of(9, 30));

        when(usuarioRepository.bloquearPorId(12L)).thenReturn(Optional.of(doctor()));
        when(citaRepository.buscarCupoOcupado(12L, manana, LocalTime.of(9, 30), null)).thenReturn(List.of());
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita guardada = citaService.crear(cita);

        assertEquals("AGENDADA", guardada.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void reagendarRechazaUnHorarioOcupadoDeOtroPaciente() {
        Cita cita = cita(manana, LocalTime.of(9, 0));
        LocalTime nuevaHora = LocalTime.of(10, 0);

        when(citaRepository.findById(500L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.bloquearPorId(12L)).thenReturn(Optional.of(doctor()));
        when(citaRepository.buscarCupoOcupado(12L, manana, nuevaHora, 500L))
                .thenReturn(List.of(cita(manana, nuevaHora)));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> citaService.reagendar(500L, manana, nuevaHora, "prueba"));

        assertTrue(error.getMessage().contains("ya tiene una cita en ese horario"),
                "El mensaje debe explicar el choque, fue: " + error.getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    void reagendarPermiteMoverUnaCitaASuPropioHorario() {
        Cita cita = cita(manana, LocalTime.of(9, 0));

        when(citaRepository.findById(500L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.bloquearPorId(12L)).thenReturn(Optional.of(doctor()));
        // La propia cita no debe contar como ocupación de sí misma
        when(citaRepository.buscarCupoOcupado(12L, manana, LocalTime.of(9, 0), 500L)).thenReturn(List.of());
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listaEsperaRepository.findByServicioInteresAndNotificadoFalse(anyString())).thenReturn(List.of());

        Cita movida = citaService.reagendar(500L, manana, LocalTime.of(9, 0), "mismo horario");

        assertEquals("REAGENDADA", movida.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void reagendarConsultaElCupoExcluyendoLaPropiaCita() {
        Cita cita = cita(manana, LocalTime.of(9, 0));
        LocalTime nuevaHora = LocalTime.of(11, 0);

        when(citaRepository.findById(500L)).thenReturn(Optional.of(cita));
        when(usuarioRepository.bloquearPorId(anyLong())).thenReturn(Optional.of(doctor()));
        when(citaRepository.buscarCupoOcupado(12L, manana, nuevaHora, 500L)).thenReturn(List.of());
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listaEsperaRepository.findByServicioInteresAndNotificadoFalse(anyString())).thenReturn(List.of());

        citaService.reagendar(500L, manana, nuevaHora, "prueba");

        verify(citaRepository).buscarCupoOcupado(12L, manana, nuevaHora, 500L);
    }
}
