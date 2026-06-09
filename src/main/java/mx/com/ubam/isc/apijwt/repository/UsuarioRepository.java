package mx.com.ubam.isc.apijwt.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findByRol(String rol);
    List<Usuario> findByRolAndEspecialidad(String rol, String especialidad);
}