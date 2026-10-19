package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", unique = true)
    private Cliente paciente;

    private String estado; // ACTIVO, INACTIVO, ARCHIVADO

    @Column(length = 1000)
    private String observaciones;

    private LocalDateTime fechaActualizacion;

    public Expediente() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getPaciente() { return paciente; }
    public void setPaciente(Cliente paciente) { this.paciente = paciente; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
