package mx.com.ubam.isc.apijwt.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)

public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers("/", "/index.html", "/registro.html", "/test.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/auth/login", "/auth/logout", "/api/public/**").permitAll()
                
                // Archivos HTML (se protegerán via JavaScript)
                .requestMatchers("/admin.html", "/empleado.html", "/paciente.html", "/dashboard.html", "/doctor.html").permitAll()
                .requestMatchers("/director.html", "/jefe.html", "/psicologo.html", "/psiquiatra.html").permitAll()
                .requestMatchers("/administrativo.html", "/enfermero.html", "/trabajador-social.html").permitAll()
                .requestMatchers("/director.html").permitAll()
               
                
                // APIs requieren autenticación
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}