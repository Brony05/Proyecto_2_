package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.dto.EstadisticasDTO;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/estadisticas")
    public EstadisticasDTO obtenerEstadisticas() {
        long totalClientes = clienteRepository.count();
        long citasHoy = citaRepository.findByFecha(LocalDate.now()).size();
        
        Map<LocalDate, Long> citasPorDia = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = LocalDate.now().minusDays(i);
            citasPorDia.put(fecha, (long) citaRepository.findByFecha(fecha).size());
        }
        
        return new EstadisticasDTO(totalClientes, citasHoy, citasPorDia);
    }
}