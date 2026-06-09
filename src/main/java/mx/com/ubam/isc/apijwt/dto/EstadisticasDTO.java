package mx.com.ubam.isc.apijwt.dto;

import java.time.LocalDate;
import java.util.Map;

public class EstadisticasDTO {
    private long totalClientes;
    private long citasHoy;
    private Map<LocalDate, Long> citasPorDia;

    public EstadisticasDTO(long totalClientes, long citasHoy, Map<LocalDate, Long> citasPorDia) {
        this.totalClientes = totalClientes;
        this.citasHoy = citasHoy;
        this.citasPorDia = citasPorDia;
    }

    // Getters
    public long getTotalClientes() { return totalClientes; }
    public long getCitasHoy() { return citasHoy; }
    public Map<LocalDate, Long> getCitasPorDia() { return citasPorDia; }
}