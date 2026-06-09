package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mx.com.ubam.isc.apijwt.model.ListaEspera;
import mx.com.ubam.isc.apijwt.model.Cliente;
import java.util.List;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {
    List<ListaEspera> findByServicioInteresAndNotificadoFalse(String servicioInteres);
    List<ListaEspera> findByPaciente(Cliente paciente);
    boolean existsByPacienteAndServicioInteres(Cliente paciente, String servicioInteres);
    void deleteByPaciente(Cliente paciente);
}