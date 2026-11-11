package mx.com.ubam.isc.apijwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import mx.com.ubam.isc.apijwt.model.Cita;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    
    List<Cita> findByFecha(LocalDate fecha);
    
    @Query("SELECT c FROM Cita c WHERE c.fecha = :fecha AND c.hora = :hora AND c.estado != 'CANCELADA'")
    List<Cita> verificarHorarioOcupado(@Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora);
    
    List<Cita> findByFechaBetween(LocalDate inicio, LocalDate fin);
    List<Cita> findByClienteId(Long clienteId);
    List<Cita> findByEstado(String estado);
    List<Cita> findByEstadoAndFechaBetween(String estado, LocalDate inicio, LocalDate fin);
    List<Cita> findByDoctorId(Long doctorId);
    List<Cita> findByDoctorIdAndFecha(Long doctorId, LocalDate fecha);

    /**
     * Citas activas de un especialista en un horario concreto. :excluir permite ignorar
     * la propia cita al reagendar al mismo horario (si no, se bloquearía a sí misma).
     */
    @Query("SELECT c FROM Cita c WHERE c.doctor.id = :doctorId AND c.fecha = :fecha AND c.hora = :hora " +
           "AND (c.estado IS NULL OR c.estado <> 'CANCELADA') AND c.id <> COALESCE(:excluir, -1)")
    List<Cita> buscarCupoOcupado(@Param("doctorId") Long doctorId,
                                 @Param("fecha") LocalDate fecha,
                                 @Param("hora") LocalTime hora,
                                 @Param("excluir") Long excluir);
}