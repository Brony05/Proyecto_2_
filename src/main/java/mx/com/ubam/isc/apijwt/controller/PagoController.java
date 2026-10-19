package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import mx.com.ubam.isc.apijwt.model.Pago;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.repository.PagoRepository;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caja real del consultorio: cada cita COMPLETADA se cobra exactamente una vez,
 * con método de pago, referencia y usuario que registró el cobro.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private static final String ROLES_CAJA =
        "hasRole('ADMINISTRATIVO') or hasRole('EMPLEADO') or hasRole('ADMIN') or hasRole('DIRECTOR')";

    private static final List<String> METODOS_VALIDOS = List.of("Efectivo", "Tarjeta", "Transferencia");

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    private Map<String, Object> pagoDto(Pago p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", p.getId());
        dto.put("citaId", p.getCita() != null ? p.getCita().getId() : null);
        dto.put("pacienteNombre", p.getCita() != null && p.getCita().getCliente() != null
                ? p.getCita().getCliente().getNombre() : null);
        dto.put("servicio", p.getCita() != null ? p.getCita().getServicio() : null);
        dto.put("citaFecha", p.getCita() != null ? p.getCita().getFecha() : null);
        dto.put("monto", p.getMonto());
        dto.put("metodo", p.getMetodo());
        dto.put("referencia", p.getReferencia());
        dto.put("fecha", p.getFecha());
        dto.put("registradoPor", p.getRegistradoPor());
        return dto;
    }

    @GetMapping
    @PreAuthorize(ROLES_CAJA)
    public ResponseEntity<?> listar() {
        try {
            List<Map<String, Object>> pagos = pagoRepository.findAllByOrderByFechaDesc()
                    .stream().map(this::pagoDto).collect(Collectors.toList());
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar pagos: " + e.getMessage());
        }
    }

    /** Pagos de citas atendidas dentro de un rango de fechas (para facturación). */
    @GetMapping("/rango")
    @PreAuthorize(ROLES_CAJA)
    public ResponseEntity<?> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        try {
            List<Map<String, Object>> pagos = pagoRepository.findByCitaFechaBetween(inicio, fin)
                    .stream().map(this::pagoDto).collect(Collectors.toList());
            return ResponseEntity.ok(pagos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al listar pagos: " + e.getMessage());
        }
    }

    /** Registra el cobro de una cita COMPLETADA. Cada cita solo puede pagarse una vez. */
    @PostMapping
    @PreAuthorize(ROLES_CAJA)
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> payload) {
        try {
            Long citaId = payload.get("citaId") != null ? ((Number) payload.get("citaId")).longValue() : null;
            Double monto = payload.get("monto") != null ? ((Number) payload.get("monto")).doubleValue() : null;
            String metodo = (String) payload.get("metodo");
            String referencia = (String) payload.get("referencia");

            if (citaId == null) {
                return ResponseEntity.badRequest().body("La cita es obligatoria");
            }
            if (monto == null || monto <= 0) {
                return ResponseEntity.badRequest().body("El monto debe ser mayor a cero");
            }
            if (metodo == null || !METODOS_VALIDOS.contains(metodo)) {
                return ResponseEntity.badRequest().body("El método de pago debe ser Efectivo, Tarjeta o Transferencia");
            }

            Cita cita = citaRepository.findById(citaId).orElse(null);
            if (cita == null) {
                return ResponseEntity.badRequest().body("Cita no encontrada");
            }
            if (!"COMPLETADA".equals(cita.getEstado())) {
                return ResponseEntity.badRequest().body("Solo se pueden cobrar citas completadas");
            }
            if (pagoRepository.existsByCitaId(citaId)) {
                return ResponseEntity.badRequest().body("Esta cita ya tiene un pago registrado");
            }

            Pago pago = new Pago();
            pago.setCita(cita);
            pago.setMonto(monto);
            pago.setMetodo(metodo);
            pago.setReferencia(referencia != null && !referencia.isEmpty() ? referencia : "Sin referencia");
            pago.setFecha(LocalDateTime.now());
            pago.setRegistradoPor(AuditoriaService.usuarioActual());
            pagoRepository.save(pago);

            String paciente = cita.getCliente() != null ? cita.getCliente().getNombre() : "ID " + citaId;
            auditoriaService.registrar(AuditoriaService.usuarioActual(), "PAGO",
                    String.format("Cobro de $%.2f (%s) por cita ID %d del paciente '%s'",
                            monto, metodo, citaId, paciente));

            Map<String, Object> response = new HashMap<>(pagoDto(pago));
            response.put("mensaje", "Pago registrado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar pago: " + e.getMessage());
        }
    }
}
