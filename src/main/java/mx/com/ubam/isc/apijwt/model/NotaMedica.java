package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class NotaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "cita_id")
    private Cita cita;
    
    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Cliente paciente;
    
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Usuario doctor;
    
    @Column(length = 2000)
    private String diagnostico;
    
    @Column(length = 2000)
    private String recomendaciones;
    
    @Column(length = 1000)
    private String medicamentos;
    
    private LocalDateTime fechaCreacion;
    
    private String tipo; // DIAGNOSTICO, RECOMENDACION, MEDICAMENTO, CONSEJO
    
    public NotaMedica() {}
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }
    public Cliente getPaciente() { return paciente; }
    public void setPaciente(Cliente paciente) { this.paciente = paciente; }
    public Usuario getDoctor() { return doctor; }
    public void setDoctor(Usuario doctor) { this.doctor = doctor; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getRecomendaciones() { return recomendaciones; }
    public void setRecomendaciones(String recomendaciones) { this.recomendaciones = recomendaciones; }
    public String getMedicamentos() { return medicamentos; }
    public void setMedicamentos(String medicamentos) { this.medicamentos = medicamentos; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}