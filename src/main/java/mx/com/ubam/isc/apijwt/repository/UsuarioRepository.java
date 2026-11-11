package mx.com.ubam.isc.apijwt.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mx.com.ubam.isc.apijwt.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findByRol(String rol);
    List<Usuario> findByRolAndEspecialidad(String rol, String especialidad);

    /**
     * Bloquea la fila del especialista para serializar dos reservas simultáneas
     * del mismo horario: la segunda espera a que la primera confirme.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> bloquearPorId(@Param("id") Long id);
}