package mx.com.ubam.isc.apijwt.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    /**
     * Clave de firma de los tokens. Ya no tiene valor por defecto: un secreto
     * incrustado en el código es el que está en el historial de Git (SC-01).
     * Si falta, Spring no levanta el contexto y el arranque falla con aviso claro.
     */
    private final String SECRET;

    public JwtUtil(@Value("${JWT_SECRET}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "Falta JWT_SECRET: define el secreto en .env (ver docs/secretos.md) o en el entorno.");
        }
        this.SECRET = secret;
    }
    
    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
    
    public String generarToken(String username, String rol) {
        String token = Jwts.builder()
                .setSubject(username)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 horas
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
        System.out.println("Token generado para " + username + " con rol " + rol);
        return token;
    }
    
    public String extraerUsuario(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    public String extraerRol(String token) {
        try {
            String rol = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("rol", String.class);
            System.out.println("Rol extraído del token: " + rol);
            return rol;
        } catch (Exception e) {
            System.err.println("Error extrayendo rol: " + e.getMessage());
            return null;
        }
    }
    
    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}