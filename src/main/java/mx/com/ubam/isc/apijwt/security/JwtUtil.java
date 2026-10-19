package mx.com.ubam.isc.apijwt.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    // El secreto se lee de variable de entorno JWT_SECRET; el fallback mantiene los tokens existentes válidos
    private final String SECRET = System.getenv("JWT_SECRET") != null 
            ? System.getenv("JWT_SECRET") 
            : "mi_clave_secreta_super_segura_12345678901234567890";
    
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