package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import mx.com.ubam.isc.apijwt.model.TokenInvalido;
import mx.com.ubam.isc.apijwt.model.Usuario;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenInvalidoRepository extends JpaRepository<TokenInvalido, Long> {
    
    Optional<TokenInvalido> findByToken(String token);
    
    @Transactional
    void deleteByUsuario(Usuario usuario);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenInvalido t WHERE t.fechaInvalido < :fechaLimite")
    void deleteTokensAntiguos(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    boolean existsByToken(String token);
}