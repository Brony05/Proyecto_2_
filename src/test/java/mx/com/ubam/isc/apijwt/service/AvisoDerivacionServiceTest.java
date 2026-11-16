package mx.com.ubam.isc.apijwt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;

/**
 * SC-07: cuando llega una derivación pendiente, el jefe del departamento destino
 * recibe un correo con el paciente y el motivo.
 */
class AvisoDerivacionServiceTest {

    private UsuarioRepository usuarioRepository;
    private EmailService emailService;
    private AvisoDerivacionService servicio;

    private final String buzon = "buzon@consultorio.test";

    @BeforeEach
    void preparar() {
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        emailService = Mockito.mock(EmailService.class);
        servicio = new AvisoDerivacionService(usuarioRepository, emailService, buzon);
    }

    private Usuario jefe(String correo) {
        Usuario jefe = new Usuario();
        jefe.setId(23L);
        jefe.setUsername("jefe.psicologia");
        jefe.setNombreCompleto("Lic. Ana Ruiz");
        jefe.setRol("JEFE_PSICOLOGIA");
        jefe.setDepartamento("PSICOLOGIA");
        jefe.setCorreo(correo);
        return jefe;
    }

    private Usuario solicitante() {
        Usuario u = new Usuario();
        u.setUsername("enfermero1");
        u.setNombreCompleto("Enf. José Ramírez");
        u.setRol("ENFERMERO");
        return u;
    }

    private Cliente paciente() {
        Cliente c = new Cliente();
        c.setId(12L);
        c.setNombre("Cristian Bedolla Fragoso");
        c.setTelefono("5551234567");
        return c;
    }

    @Test
    void avisaAlJefeConPacienteYMotivo() {
        when(usuarioRepository.findByRol("JEFE_PSICOLOGIA")).thenReturn(List.of(jefe("ana.ruiz@clinica.test")));

        int enviados = servicio.avisar("ENFERMERIA", "PSICOLOGIA", paciente(),
                "Requiere valoración psicológica por duelo", solicitante());

        assertEquals(1, enviados);

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarRecordatorio(eq("ana.ruiz@clinica.test"),
                contains("Cristian Bedolla Fragoso"), cuerpo.capture());

        String texto = cuerpo.getValue();
        assertTrue(texto.contains("Requiere valoración psicológica por duelo"), "falta el motivo: " + texto);
        assertTrue(texto.contains("ENFERMERIA") && texto.contains("PSICOLOGIA"), "faltan los departamentos");
        assertTrue(texto.contains("Enf. José Ramírez"), "falta quién solicita");
        assertTrue(texto.contains("Derivaciones"), "debe decir dónde resolverla");
    }

    @Test
    void sinCorreoDelJefeUsaElBuzonDelConsultorioYAvisaDeEllo() {
        when(usuarioRepository.findByRol("JEFE_PSICOLOGIA")).thenReturn(List.of(jefe(null)));

        int enviados = servicio.avisar("ENFERMERIA", "PSICOLOGIA", paciente(), "Control", solicitante());

        assertEquals(1, enviados);
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarRecordatorio(eq(buzon), anyString(), cuerpo.capture());
        assertTrue(cuerpo.getValue().contains("no tiene correo configurado"),
                "debe explicar por qué llegó al buzón: " + cuerpo.getValue());
    }

    @Test
    void sinJefesNoEnviaNada() {
        when(usuarioRepository.findByRol("JEFE_PSIQUIATRIA")).thenReturn(List.of());

        int enviados = servicio.avisar("ENFERMERIA", "PSIQUIATRIA", paciente(), "Control", solicitante());

        assertEquals(0, enviados);
        verify(emailService, never()).enviarRecordatorio(anyString(), anyString(), anyString());
    }

    @Test
    void departamentoSinJefeConocidoNoEnvia() {
        int enviados = servicio.avisar("ENFERMERIA", "ARCHIVO", paciente(), "Control", solicitante());
        assertEquals(0, enviados);
        verify(emailService, never()).enviarRecordatorio(anyString(), anyString(), anyString());
    }
}
