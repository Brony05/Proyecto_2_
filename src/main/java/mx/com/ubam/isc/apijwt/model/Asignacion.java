package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Vínculo formal entre un paciente y un profesional del personal clínico.
 * A diferencia de la deducción por citas, esta tabla es la fuente autoritativa
 * para saber qué pacientes le tocan a cada enfermero, psicólogo, psiquiatra o
 * trabajador social.
 *
 * - La crea el jefe del departamento (o director/admin) desde su panel.
 * - También se auto-crea cuando se agenda una cita con ese profesional.
 * - "activa" permite liberar una asignación sin borrar el historial.
 */
@Entity
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Departamento al que pertenece el profesional (PSICOLOGIA, ENFERMERIA, ...). */
    private String departamento;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa = true;

    /** Motivo de la asignación (ej: "Seguimiento de caso", "Toma de signos vitales"). */
    private String motivo;

    public Asignacion() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
