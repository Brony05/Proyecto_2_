package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Servicio;
import mx.com.ubam.isc.apijwt.repository.ServicioRepository;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    @Autowired
    private ServicioRepository servicioRepository;

    @GetMapping
    public List<Servicio> listar() {
        return servicioRepository.findByActivoTrueOrderByNombreAsc();
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Servicio> listarTodos() {
        return servicioRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Servicio> obtener(@PathVariable Long id) {
        return servicioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Servicio crear(@RequestBody Servicio servicio) {
        return servicioRepository.save(servicio);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Servicio> actualizar(@PathVariable Long id, @RequestBody Servicio servicio) {
        if (!servicioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        servicio.setId(id);
        return ResponseEntity.ok(servicioRepository.save(servicio));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!servicioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        servicioRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}