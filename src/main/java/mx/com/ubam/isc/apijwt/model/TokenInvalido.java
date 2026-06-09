package mx.com.ubam.isc.apijwt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_invalido")
public class TokenInvalido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String token;

    private LocalDateTime fechaInvalido;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Constructor por defecto (obligatorio para JPA)
    public TokenInvalido() {
    }

    // Constructor con token y usuario (EL QUE NECESITAS)
    public TokenInvalido(String token, Usuario usuario) {
        this.token = token;
        this.usuario = usuario;
        this.fechaInvalido = LocalDateTime.now();
    }

    // Constructor con todos los parámetros
    public TokenInvalido(String token, LocalDateTime fechaInvalido, Usuario usuario) {
        this.token = token;
        this.fechaInvalido = fechaInvalido;
        this.usuario = usuario;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getFechaInvalido() {
        return fechaInvalido;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setFechaInvalido(LocalDateTime fechaInvalido) {
        this.fechaInvalido = fechaInvalido;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public String toString() {
        return "TokenInvalido{" +
                "id=" + id +
                ", token='" + (token != null ? token.substring(0, Math.min(token.length(), 50)) : "null") + "...'" +
                ", fechaInvalido=" + fechaInvalido +
                ", usuario=" + (usuario != null ? usuario.getUsername() : "null") +
                '}';
    }
}