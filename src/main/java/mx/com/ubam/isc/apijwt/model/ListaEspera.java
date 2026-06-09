package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lista_espera")
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Cliente paciente;
    
    private String servicioInteres;
    
    private LocalDate fechaDeseada;
    
    private String horarioPreferido;
    
    private LocalDateTime fechaRegistro;
    
    private Boolean notificado = false;

    public ListaEspera() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getPaciente() { return paciente; }
    public void setPaciente(Cliente paciente) { this.paciente = paciente; }
    public String getServicioInteres() { return servicioInteres; }
    public void setServicioInteres(String servicioInteres) { this.servicioInteres = servicioInteres; }
    public LocalDate getFechaDeseada() { return fechaDeseada; }
    public void setFechaDeseada(LocalDate fechaDeseada) { this.fechaDeseada = fechaDeseada; }
    public String getHorarioPreferido() { return horarioPreferido; }
    public void setHorarioPreferido(String horarioPreferido) { this.horarioPreferido = horarioPreferido; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public Boolean getNotificado() { return notificado; }
    public void setNotificado(Boolean notificado) { this.notificado = notificado; }
}