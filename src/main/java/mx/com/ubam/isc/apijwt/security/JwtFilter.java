package mx.com.ubam.isc.apijwt.security;

import java.io.IOException;
import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import mx.com.ubam.isc.apijwt.repository.TokenInvalidoRepository;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenInvalidoRepository tokenInvalidoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
                                    throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Permitir OPTIONS (CORS preflight)
        if ("OPTIONS".equals(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            chain.doFilter(request, response);
            return;
        }
        
        // Rutas públicas
        boolean isPublicPath = 
            path.equals("/") ||
            path.equals("/index.html") ||
            path.equals("/registro.html") ||
            path.equals("/test.html") ||
            path.equals("/admin.html") ||
            path.equals("/empleado.html") ||
            path.equals("/paciente.html") ||
            path.equals("/dashboard.html") ||
            path.equals("/doctor.html") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/auth/login") ||
            path.startsWith("/auth/logout") ||
            path.startsWith("/api/public/");
        
        if (isPublicPath) {
            chain.doFilter(request, response);
            return;
        }
        
        // Para APIs, verificar token
        if (path.startsWith("/api/")) {
            String header = request.getHeader("Authorization");
            
            if (header == null || !header.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token no proporcionado\"}");
                return;
            }
            
            String token = header.substring(7);
            
            // Verificar si token está en blacklist
            if (tokenInvalidoRepository.findByToken(token).isPresent()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token inválido\"}");
                return;
            }
            
            try {
                String username = jwtUtil.extraerUsuario(token);
                String rol = jwtUtil.extraerRol(token);
                
                System.out.println("🔵 JwtFilter - Username: " + username + ", Rol: " + rol);
                
                if (username != null && rol != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // IMPORTANTE: El rol debe tener el prefijo "ROLE_"
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + rol))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("✅ Autenticación establecida para: " + username + " con rol ROLE_" + rol);
                }
            } catch (Exception e) {
                System.err.println("❌ Error validando token: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token inválido\"}");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}