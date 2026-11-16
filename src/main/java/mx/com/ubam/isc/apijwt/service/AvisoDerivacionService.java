package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import java.util.List;

/**
 * Aviso por correo al jefe del departamento destino cuando entra una derivación
 * pendiente (SC-07).
 *
 * El jefe se entera antes solo al abrir su bandeja; este aviso lleva paciente,
 * motivo y quién solicita para que pueda decidir sin entrar primero.
 *
 * Si el usuario jefe no tiene correo propio (hoy todos están en NULL) el aviso
 * va al buzón del consultorio, con una nota que lo explica, para no perderlo.
 * El envío es best-effort: cualquier fallo se registra y la derivación ya creada
 * no se ve afectada.
 */
@Service
public class AvisoDerivacionService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final String buzonConsultorio;

    public AvisoDerivacionService(UsuarioRepository usuarioRepository,
                                  EmailService emailService,
                                  @Value("${spring.mail.username:}") String buzonConsultorio) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.buzonConsultorio = buzonConsultorio;
    }

    /** Rol del jefe que recibe los avisos de cada departamento. */
    static String rolJefeDe(String departamento) {
        if (departamento == null) return null;
        return switch (departamento) {
            case "PSICOLOGIA" -> "JEFE_PSICOLOGIA";
            case "PSIQUIATRIA" -> "JEFE_PSIQUIATRIA";
            case "TRABAJO_SOCIAL" -> "JEFE_TRABAJO_SOCIAL";
            case "ENFERMERIA" -> "JEFE_ENFERMERIA";
            default -> null;
        };
    }

    /**
     * Envía el aviso a los jefes del departamento destino.
     * @return cuántas direcciones se intentaron notificar (0 si no hay jefe o correo).
     */
    public int avisar(String origen, String destino, Cliente cliente, String motivo, Usuario solicita) {
        String rolJefe = rolJefeDe(destino);
        if (rolJefe == null || cliente == null) return 0;

        List<Usuario> jefes = usuarioRepository.findByRol(rolJefe);
        if (jefes == null || jefes.isEmpty()) return 0;

        String asunto = "Derivación pendiente: " + cliente.getNombre() + " → " + destino;
        int enviados = 0;

        for (Usuario jefe : jefes) {
            String correo = correoDe(jefe);
            if (correo == null) continue;
            try {
                emailService.enviarRecordatorio(correo, asunto,
                        cuerpoDeAviso(origen, destino, cliente, motivo, solicita, jefe, correoSinDueno(jefe)));
                enviados++;
            } catch (Exception e) {
                System.err.println("No se pudo avisar a " + jefe.getUsername() + ": " + e.getMessage());
            }
        }
        return enviados;
    }

    /** Correo del jefe; si no tiene, el buzón del consultorio (nunca inventa direcciones). */
    String correoDe(Usuario jefe) {
        if (jefe == null) return null;
        String correo = jefe.getCorreo();
        if (correo == null || correo.isBlank()) {
            String usuario = jefe.getUsername();
            if (usuario != null && usuario.contains("@")) correo = usuario;
        }
        if (correo == null || correo.isBlank()) {
            correo = (buzonConsultorio == null || buzonConsultorio.isBlank()) ? null : buzonConsultorio;
        }
        return correo;
    }

    private boolean correoSinDueno(Usuario jefe) {
        return jefe.getCorreo() == null || jefe.getCorreo().isBlank();
    }

    String cuerpoDeAviso(String origen, String destino, Cliente cliente, String motivo,
                         Usuario solicita, Usuario jefe, boolean avisoAlBuzon) {
        StringBuilder texto = new StringBuilder();
        texto.append("Hola ").append(nombreDe(jefe)).append(",\n\n");
        texto.append("Entró una derivación pendiente para ").append(destino).append(".\n\n");
        texto.append("Paciente: ").append(cliente.getNombre()).append("\n");
        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
            texto.append("Teléfono: ").append(cliente.getTelefono()).append("\n");
        }
        if (cliente.getCorreo() != null && !cliente.getCorreo().isBlank()) {
            texto.append("Correo: ").append(cliente.getCorreo()).append("\n");
        }
        texto.append("De: ").append(origen).append(" a ").append(destino).append("\n");
        texto.append("Motivo: ").append(motivo).append("\n");
        texto.append("Solicita: ").append(nombreDe(solicita)).append("\n\n");
        texto.append("Entra al panel, pestaña Derivaciones, para aprobarla o rechazarla.\n");
        if (avisoAlBuzon) {
            texto.append("\n(El usuario ").append(jefe.getUsername())
                 .append(" no tiene correo configurado, así que este aviso llegó al buzón del consultorio.)\n");
        }
        return texto.toString();
    }

    private String nombreDe(Usuario usuario) {
        if (usuario == null) return "el equipo";
        if (usuario.getNombreCompleto() != null && !usuario.getNombreCompleto().isBlank()) {
            return usuario.getNombreCompleto();
        }
        return usuario.getUsername();
    }
}
