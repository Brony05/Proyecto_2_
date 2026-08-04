package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    private String rol; 
    // ADMIN, DIRECTOR, COMITE_ETICA, JEFE_PSICOLOGIA, JEFE_PSIQUIATRIA, 
    // JEFE_TRABAJO_SOCIAL, JEFE_ENFERMERIA, JEFE_ARCHIVO, PSICOLOGO, 
    // PSIQUIATRA, ENFERMERO, TRABAJADOR_SOCIAL, ADMINISTRATIVO, PACIENTE
    
    private String especialidad;
    private String nombreCompleto;
    private String telefono;
    private String correo;

    // Nuevo campo para el departamento al que pertenece
    private String departamento; 
    // PSICOLOGIA, PSIQUIATRIA, TRABAJO_SOCIAL, ENFERMERIA, ARCHIVO, ADMINISTRACION

    public Usuario() {}

    public Usuario(String username, String password, String rol) {
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
}