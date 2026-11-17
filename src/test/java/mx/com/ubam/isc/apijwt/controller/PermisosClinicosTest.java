package mx.com.ubam.isc.apijwt.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;

import mx.com.ubam.isc.apijwt.model.Asignacion;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Derivacion;
import mx.com.ubam.isc.apijwt.model.ListaEspera;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.AsignacionRepository;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.DerivacionRepository;
import mx.com.ubam.isc.apijwt.repository.ListaEsperaRepository;
import mx.com.ubam.isc.apijwt.repository.NotaMedicaRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import mx.com.ubam.isc.apijwt.service.AvisoDerivacionService;
import mx.com.ubam.isc.apijwt.service.CitaService;

/**
 * SC-02: red de seguridad sobre los permisos clínicos.
 *
 * Los tres huecos que se cerraron a mano (cita por id, exportación de PDF y
 * lectura de notas) se comprobaban con curl; si alguien relajaba una condición,
 * nada fallaba. Estas pruebas fijan las reglas de propiedad en el código:
 * quien no es el paciente, ni su profesional, ni administración, no ve nada.
 *
 * La exportación de PDF es la excepción al patrón: su regla vive en un
 * @PreAuthorize, que no se evalúa llamando al controlador de forma directa, así
 * que ese caso se prueba a través de un proxy con seguridad de métodos activa.
 *
 * Son pruebas de unidad con repositorios simulados: no necesitan base de datos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermisosClinicosTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private AsignacionRepository asignacionRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private DerivacionRepository derivacionRepository;
    @Mock private ListaEsperaRepository listaEsperaRepository;
    @Mock private NotaMedicaRepository notaRepository;
    @Mock private CitaService citaService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private AvisoDerivacionService avisoDerivacionService;

    @InjectMocks private CitaController citaController;
    @InjectMocks private NotaMedicaController notaController;
    @InjectMocks private DerivacionController derivacionController;
    @InjectMocks private AsignacionController asignacionController;

    private final Long PACIENTE_ID = 12L;
    private final Long OTRO_PACIENTE_ID = 99L;

    @BeforeEach
    void prepararSesion() {
        sesion("alguien");
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void sesion(String username) {
        sesionCon(username, List.of());
    }

    private void sesionCon(String username, List<GrantedAuthority> permisos) {
        Authentication auth = new UsernamePasswordAuthenticationToken(username, "n/a", permisos);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Usuario usuario(Long id, String username, String rol, String departamento) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setRol(rol);
        u.setDepartamento(departamento);
        u.setNombreCompleto(username);
        return u;
    }

    private Cliente cliente(Long id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombre("Paciente " + id);
        c.setCorreo("paciente" + id + "@correo.test");
        return c;
    }

    private Cita citaDe(Cliente cliente, Usuario doctor) {
        Cita cita = new Cita();
        cita.setId(500L);
        cita.setCliente(cliente);
        cita.setDoctor(doctor);
        cita.setServicio("Seguimiento");
        return cita;
    }

    // ==================== CITA POR ID ====================

    @Test
    void unPacienteNoPuedeLeerLaCitaDeOtro() {
        Usuario paciente = usuario(42L, "paciente1", "PACIENTE", null);
        when(usuarioRepository.findByUsername("paciente1")).thenReturn(Optional.of(paciente));
        when(clienteRepository.findByCorreo("paciente1")).thenReturn(cliente(OTRO_PACIENTE_ID));
        when(citaService.obtenerPorId(500L))
                .thenReturn(Optional.of(citaDe(cliente(PACIENTE_ID), usuario(12L, "Juan", "DOCTOR", "PSICOLOGIA"))));

        sesion("paciente1");
        ResponseEntity<?> respuesta = citaController.obtener(500L);

        assertEquals(403, respuesta.getStatusCode().value());
        assertTrue(String.valueOf(respuesta.getBody()).contains("no te pertenece"));
    }

    @Test
    void unPacienteSiPuedeLeerSuPropiaCita() {
        Usuario paciente = usuario(42L, "paciente1", "PACIENTE", null);
        when(usuarioRepository.findByUsername("paciente1")).thenReturn(Optional.of(paciente));
        when(clienteRepository.findByCorreo("paciente1")).thenReturn(cliente(PACIENTE_ID));
        when(citaService.obtenerPorId(500L))
                .thenReturn(Optional.of(citaDe(cliente(PACIENTE_ID), usuario(12L, "Juan", "DOCTOR", "PSICOLOGIA"))));

        sesion("paciente1");
        assertEquals(200, citaController.obtener(500L).getStatusCode().value());
    }

    @Test
    void unProfesionalQueNoAtiendeLaCitaNoPuedeLeerla() {
        Usuario psicologo = usuario(29L, "psicologo1", "PSICOLOGO", "PSICOLOGIA");
        when(usuarioRepository.findByUsername("psicologo1")).thenReturn(Optional.of(psicologo));
        when(citaService.obtenerPorId(500L))
                .thenReturn(Optional.of(citaDe(cliente(PACIENTE_ID), usuario(12L, "Juan", "DOCTOR", "PSICOLOGIA"))));

        sesion("psicologo1");
        ResponseEntity<?> respuesta = citaController.obtener(500L);

        assertEquals(403, respuesta.getStatusCode().value());
        assertTrue(String.valueOf(respuesta.getBody()).contains("no está asignada a ti"));
    }

    @Test
    void administracionPuedeLeerCualquierCita() {
        Usuario admin = usuario(1L, "admin", "ADMIN", "ADMINISTRACION");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(citaService.obtenerPorId(500L))
                .thenReturn(Optional.of(citaDe(cliente(PACIENTE_ID), usuario(12L, "Juan", "DOCTOR", "PSICOLOGIA"))));

        sesion("admin");
        assertEquals(200, citaController.obtener(500L).getStatusCode().value());
    }

    @Test
    void sinSesionValidaNoSeLeeLaCita() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertEquals(401, citaController.obtener(500L).getStatusCode().value());
    }

    // ==================== LECTURA DE NOTAS ====================

    @Test
    void unProfesionalSinVinculoNoLeeLasNotasDelPaciente() {
        Usuario psicologo = usuario(29L, "psicologo1", "PSICOLOGO", "PSICOLOGIA");
        when(usuarioRepository.findByUsername("psicologo1")).thenReturn(Optional.of(psicologo));
        when(asignacionRepository.existsByUsuarioIdAndClienteIdAndActivaTrue(29L, PACIENTE_ID)).thenReturn(false);
        when(citaRepository.findByDoctorId(29L)).thenReturn(List.of());

        sesion("psicologo1");
        ResponseEntity<?> respuesta = notaController.getNotasPorPaciente(PACIENTE_ID);

        assertEquals(403, respuesta.getStatusCode().value());
        assertTrue(String.valueOf(respuesta.getBody()).contains("no está asignado a ti"));
    }

    @Test
    void conAsignacionActivaSiPuedeLeerLasNotas() {
        Usuario psicologo = usuario(29L, "psicologo1", "PSICOLOGO", "PSICOLOGIA");
        when(usuarioRepository.findByUsername("psicologo1")).thenReturn(Optional.of(psicologo));
        when(asignacionRepository.existsByUsuarioIdAndClienteIdAndActivaTrue(29L, PACIENTE_ID)).thenReturn(true);
        when(notaRepository.findByPacienteId(PACIENTE_ID)).thenReturn(List.of());

        sesion("psicologo1");
        assertEquals(200, notaController.getNotasPorPaciente(PACIENTE_ID).getStatusCode().value());
    }

    // ==================== DERIVACIONES ====================

    @Test
    void noSeDerivaUnPacienteQueNoEsTuyo() {
        Usuario enfermero = usuario(35L, "enfermero1", "ENFERMERO", "ENFERMERIA");
        when(usuarioRepository.findByUsername("enfermero1")).thenReturn(Optional.of(enfermero));
        when(clienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(cliente(PACIENTE_ID)));
        when(asignacionRepository.existsByUsuarioIdAndClienteIdAndActivaTrue(anyLong(), anyLong())).thenReturn(false);
        when(citaRepository.findByDoctorId(anyLong())).thenReturn(List.of());

        Map<String, Object> payload = new HashMap<>();
        payload.put("clienteId", PACIENTE_ID);
        payload.put("departamentoDestino", "PSICOLOGIA");
        payload.put("motivo", "Requiere valoración");

        sesion("enfermero1");
        ResponseEntity<?> respuesta = derivacionController.solicitar(payload);

        assertEquals(403, respuesta.getStatusCode().value());
        assertTrue(String.valueOf(respuesta.getBody()).contains("pacientes que tengas asignados"));
    }

    @Test
    void noSeDerivaAlMismoDepartamento() {
        Usuario psicologo = usuario(29L, "psicologo1", "PSICOLOGO", "PSICOLOGIA");
        when(usuarioRepository.findByUsername("psicologo1")).thenReturn(Optional.of(psicologo));
        when(clienteRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(cliente(PACIENTE_ID)));
        when(asignacionRepository.existsByUsuarioIdAndClienteIdAndActivaTrue(29L, PACIENTE_ID)).thenReturn(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("clienteId", PACIENTE_ID);
        payload.put("departamentoDestino", "PSICOLOGIA");
        payload.put("motivo", "Prueba");

        sesion("psicologo1");
        assertEquals(400, derivacionController.solicitar(payload).getStatusCode().value());
    }

    @Test
    void elJefeSoloVeLaBandejaDeSuDepartamento() {
        Usuario jefe = usuario(23L, "jefe.psicologia", "JEFE_PSICOLOGIA", "PSICOLOGIA");
        when(usuarioRepository.findByUsername("jefe.psicologia")).thenReturn(Optional.of(jefe));
        when(derivacionRepository.findByDepartamentoDestinoOrderByFechaSolicitudDesc("PSICOLOGIA"))
                .thenReturn(List.of(new Derivacion()));

        sesion("jefe.psicologia");
        assertEquals(200, derivacionController.bandeja().getStatusCode().value());
    }

    // ============== CAMBIO DE DEPARTAMENTO DESDE LA LISTA DE ESPERA ==============

    private ListaEspera solicitudDeEspera(Long id, Long pacienteId) {
        ListaEspera solicitud = new ListaEspera();
        solicitud.setId(id);
        solicitud.setPaciente(cliente(pacienteId));
        solicitud.setServicioInteres("Seguimiento");
        return solicitud;
    }

    @Test
    void unPacienteNoPideCambioSobreLaEsperaDeOtro() {
        Usuario paciente = usuario(42L, "paciente1", "PACIENTE", null);
        when(usuarioRepository.findByUsername("paciente1")).thenReturn(Optional.of(paciente));
        when(clienteRepository.findByCorreo("paciente1")).thenReturn(cliente(PACIENTE_ID));
        when(listaEsperaRepository.findById(7L)).thenReturn(Optional.of(solicitudDeEspera(7L, OTRO_PACIENTE_ID)));

        Map<String, Object> payload = new HashMap<>();
        payload.put("solicitudId", 7L);
        payload.put("departamentoDestino", "PSIQUIATRIA");
        payload.put("motivo", "Necesito medicación");

        sesion("paciente1");
        ResponseEntity<?> respuesta = derivacionController.desdeListaEspera(payload);

        assertEquals(403, respuesta.getStatusCode().value());
        assertTrue(String.valueOf(respuesta.getBody()).contains("tu propia solicitud"));
    }

    @Test
    void unPacientePideCambioDesdeSuPropiaListaDeEspera() {
        Usuario paciente = usuario(42L, "paciente1", "PACIENTE", null);
        when(usuarioRepository.findByUsername("paciente1")).thenReturn(Optional.of(paciente));
        when(clienteRepository.findByCorreo("paciente1")).thenReturn(cliente(PACIENTE_ID));
        when(listaEsperaRepository.findById(7L)).thenReturn(Optional.of(solicitudDeEspera(7L, PACIENTE_ID)));
        when(derivacionRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("solicitudId", 7L);
        payload.put("departamentoDestino", "PSIQUIATRIA");
        payload.put("motivo", "Necesito valoración psiquiátrica");

        sesion("paciente1");
        ResponseEntity<?> respuesta = derivacionController.desdeListaEspera(payload);

        assertEquals(200, respuesta.getStatusCode().value());
        Derivacion creada = (Derivacion) respuesta.getBody();
        assertEquals(Derivacion.PENDIENTE, creada.getEstado());
        assertEquals("LISTA_ESPERA", creada.getDepartamentoOrigen());
        assertEquals("PSIQUIATRIA", creada.getDepartamentoDestino());
        assertEquals(PACIENTE_ID, creada.getCliente().getId());
    }

    // ==================== ASIGNACIONES ====================

    @Test
    void unJefeNoVeLasAsignacionesDeOtroDepartamento() {
        Usuario jefe = usuario(23L, "jefe.psicologia", "JEFE_PSICOLOGIA", "PSICOLOGIA");
        Usuario ajeno = usuario(32L, "psiquiatra1", "PSIQUIATRA", "PSIQUIATRIA");
        when(usuarioRepository.findByUsername("jefe.psicologia")).thenReturn(Optional.of(jefe));
        when(usuarioRepository.findById(32L)).thenReturn(Optional.of(ajeno));

        sesion("jefe.psicologia");
        assertEquals(403, asignacionController.asignacionesDePersona(32L).getStatusCode().value());
    }

    @Test
    void unJefeSiVeLasAsignacionesDeSuEquipo() {
        Usuario jefe = usuario(23L, "jefe.psicologia", "JEFE_PSICOLOGIA", "PSICOLOGIA");
        Usuario propio = usuario(29L, "psicologo1", "PSICOLOGO", "PSICOLOGIA");
        when(usuarioRepository.findByUsername("jefe.psicologia")).thenReturn(Optional.of(jefe));
        when(usuarioRepository.findById(29L)).thenReturn(Optional.of(propio));
        when(asignacionRepository.findByUsuarioIdAndActivaTrue(29L)).thenReturn(List.of(new Asignacion()));

        sesion("jefe.psicologia");
        assertEquals(200, asignacionController.asignacionesDePersona(29L).getStatusCode().value());
    }

    // ==================== EXPORTACIÓN DE PDF ====================

    /**
     * La regla del PDF vive en un @PreAuthorize, no en el cuerpo del método, así
     * que llamar al controlador no la evalúa. Aquí se toma la anotación real y se
     * evalúa con el mismo parser y contexto de Spring Security que usa el
     * interceptor, de modo que un cambio en la lista de roles rompe la prueba.
     * No cubre el cableado del interceptor: solo la regla declarada.
     */
    private boolean puedeExportarPdf(String... roles) throws Exception {
        Method metodo = CitaController.class.getMethod("exportarPdf",
                LocalDate.class, LocalDate.class, String.class, String.class,
                jakarta.servlet.http.HttpServletResponse.class);

        PreAuthorize regla = metodo.getAnnotation(PreAuthorize.class);
        assertNotNull(regla, "exportarPdf debe declarar @PreAuthorize");

        List<GrantedAuthority> permisos = new ArrayList<>();
        for (String rol : roles) {
            permisos.add(new SimpleGrantedAuthority(rol));
        }
        Authentication autenticacion = new UsernamePasswordAuthenticationToken("quien-sea", "n/a", permisos);

        SecurityExpressionHandler<MethodInvocation> manejador = new DefaultMethodSecurityExpressionHandler();
        MethodInvocation invocacion = new SimpleMethodInvocation(citaController, metodo,
                new Object[]{ LocalDate.now().minusDays(7), LocalDate.now(), "todas", "Bearer token",
                              new MockHttpServletResponse() });

        EvaluationContext contexto = manejador.createEvaluationContext(autenticacion, invocacion);
        Expression expresion = manejador.getExpressionParser().parseExpression(regla.value());
        return Boolean.TRUE.equals(expresion.getValue(contexto, Boolean.class));
    }

    @Test
    void soloAdministracionYDireccionExportanElPdf() throws Exception {
        assertTrue(puedeExportarPdf("ROLE_ADMIN"), "ADMIN debe poder exportar");
        assertTrue(puedeExportarPdf("ROLE_DIRECTOR"), "DIRECTOR debe poder exportar");

        assertFalse(puedeExportarPdf("ROLE_PSICOLOGO"), "un psicólogo no exporta el historial completo");
        assertFalse(puedeExportarPdf("ROLE_PSICOLOGO", "ROLE_ENFERMERO"), "sumar roles clínicos no habilita");
        assertFalse(puedeExportarPdf("ROLE_PACIENTE"), "un paciente no exporta el historial");
        assertFalse(puedeExportarPdf("ROLE_JEFE_PSICOLOGIA"), "el jefe de departamento tampoco");
        assertFalse(puedeExportarPdf(), "sin permisos no se exporta");
    }
}
