package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;
    private LocalTime hora;
    private String servicio;
    private Double precio;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Usuario doctor;

    private String estado;
    
    private String asistencia; // PRESENTE, AUSENTE, PENDIENTE
    
    private LocalDate fechaOriginal;
    private LocalTime horaOriginal;
    private String motivoReagendado;
    
    private String acompananteNombre;
    private String acompananteTelefono;
    private String acompananteCorreo;
    private String acompananteRelacion;

    public Cita() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Usuario getDoctor() { return doctor; }
    public void setDoctor(Usuario doctor) { this.doctor = doctor; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getAsistencia() { return asistencia; }
    public void setAsistencia(String asistencia) { this.asistencia = asistencia; }
    public LocalDate getFechaOriginal() { return fechaOriginal; }
    public void setFechaOriginal(LocalDate fechaOriginal) { this.fechaOriginal = fechaOriginal; }
    public LocalTime getHoraOriginal() { return horaOriginal; }
    public void setHoraOriginal(LocalTime horaOriginal) { this.horaOriginal = horaOriginal; }
    public String getMotivoReagendado() { return motivoReagendado; }
    public void setMotivoReagendado(String motivoReagendado) { this.motivoReagendado = motivoReagendado; }
    public String getAcompananteNombre() { return acompananteNombre; }
    public void setAcompananteNombre(String acompananteNombre) { this.acompananteNombre = acompananteNombre; }
    public String getAcompananteTelefono() { return acompananteTelefono; }
    public void setAcompananteTelefono(String acompananteTelefono) { this.acompananteTelefono = acompananteTelefono; }
    public String getAcompananteCorreo() { return acompananteCorreo; }
    public void setAcompananteCorreo(String acompananteCorreo) { this.acompananteCorreo = acompananteCorreo; }
    public String getAcompananteRelacion() { return acompananteRelacion; }
    public void setAcompananteRelacion(String acompananteRelacion) { this.acompananteRelacion = acompananteRelacion; }
}