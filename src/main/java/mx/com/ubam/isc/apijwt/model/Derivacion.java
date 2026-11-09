package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Solicitud de derivación de un paciente entre departamentos clínicos.
 *
 * Flujo:
 *  1. Un profesional con el paciente asignado (o con cita previa) crea la
 *     solicitud indicando el departamento destino y el motivo. Estado: PENDIENTE.
 *  2. El jefe del departamento destino la aprueba —creando la asignación al
 *     profesional que elija y, opcionalmente, la cita— o la rechaza.
 *
 * Se guarda aparte de {@link Asignacion} porque una derivación es una petición
 * con historial y decisión, no un vínculo ya vigente.
 */
@Entity
public class Derivacion {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Profesional que solicita la derivación. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_solicita_id")
    private Usuario usuarioSolicita;

    private String departamentoOrigen;
    private String departamentoDestino;

    @Column(length = 1000)
    private String motivo;

    private String estado = PENDIENTE;

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;

    /** Jefe (o admin/director) que resolvió la solicitud. */
    @ManyToOne
    @JoinColumn(name = "usuario_resuelve_id")
    private Usuario usuarioResuelve;

    @Column(length = 1000)
    private String comentarioResolucion;

    /** Datos de lo que se creó al aprobar, para poder rastrearlo desde el historial. */
    private Long asignacionCreadaId;
    private Long citaCreadaId;
    private LocalDate citaFecha;
    private LocalTime citaHora;

    public Derivacion() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuarioSolicita() { return usuarioSolicita; }
    public void setUsuarioSolicita(Usuario usuarioSolicita) { this.usuarioSolicita = usuarioSolicita; }
    public String getDepartamentoOrigen() { return departamentoOrigen; }
    public void setDepartamentoOrigen(String departamentoOrigen) { this.departamentoOrigen = departamentoOrigen; }
    public String getDepartamentoDestino() { return departamentoDestino; }
    public void setDepartamentoDestino(String departamentoDestino) { this.departamentoDestino = departamentoDestino; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public Usuario getUsuarioResuelve() { return usuarioResuelve; }
    public void setUsuarioResuelve(Usuario usuarioResuelve) { this.usuarioResuelve = usuarioResuelve; }
    public String getComentarioResolucion() { return comentarioResolucion; }
    public void setComentarioResolucion(String comentarioResolucion) { this.comentarioResolucion = comentarioResolucion; }
    public Long getAsignacionCreadaId() { return asignacionCreadaId; }
    public void setAsignacionCreadaId(Long asignacionCreadaId) { this.asignacionCreadaId = asignacionCreadaId; }
    public Long getCitaCreadaId() { return citaCreadaId; }
    public void setCitaCreadaId(Long citaCreadaId) { this.citaCreadaId = citaCreadaId; }
    public LocalDate getCitaFecha() { return citaFecha; }
    public void setCitaFecha(LocalDate citaFecha) { this.citaFecha = citaFecha; }
    public LocalTime getCitaHora() { return citaHora; }
    public void setCitaHora(LocalTime citaHora) { this.citaHora = citaHora; }
}
