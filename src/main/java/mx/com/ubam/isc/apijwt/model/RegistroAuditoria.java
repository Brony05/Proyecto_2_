package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario;

    private String accion; // LOGIN, CREACION, MODIFICACION, CANCELACION, EXPEDIENTE, NOTA_MEDICA, PAGO, USUARIO, ETICA

    @Column(length = 500)
    private String detalle;

    private LocalDateTime fecha;

    public RegistroAuditoria() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
