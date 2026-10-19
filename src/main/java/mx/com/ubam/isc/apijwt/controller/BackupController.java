package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.service.BackupService;
import mx.com.ubam.isc.apijwt.service.AuditoriaService;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Autowired
    private AuditoriaService auditoriaService;

    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DIRECTOR')")
    public ResponseEntity<?> realizarBackupManual() {
        try {
            String resultado = backupService.realizarBackupManual();
            auditoriaService.registrar(AuditoriaService.usuarioActual(), "BACKUP",
                "Respaldo manual de la base de datos ejecutado: " + resultado);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", resultado);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}