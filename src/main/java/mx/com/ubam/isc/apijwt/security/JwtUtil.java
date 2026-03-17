package mx.com.ubam.isc.apijwt.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final String SECRET="mi_clave_secreta_super_segura_123456";

    private Key getKey(){
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generarToken(String username){

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+86400000))
                .signWith(getKey())
                .compact();
    }

    public String extraerUsuario(String token){

        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}