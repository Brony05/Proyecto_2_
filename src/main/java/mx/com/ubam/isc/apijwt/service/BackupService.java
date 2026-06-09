package mx.com.ubam.isc.apijwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.repository.ServicioRepository;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@EnableScheduling
public class BackupService {

    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ServicioRepository servicioRepository;
    
    @Value("${backup.directory:./backups}")
    private String backupDirectory;
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void realizarBackupAutomatico() {
        try {
            System.out.println("🔄 Iniciando backup automático...");
            crearDirectorioBackup();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            exportarACsv(citaRepository.findAll(), backupDirectory + "/citas_" + timestamp + ".csv");
            exportarACsv(clienteRepository.findAll(), backupDirectory + "/clientes_" + timestamp + ".csv");
            exportarACsv(usuarioRepository.findAll(), backupDirectory + "/usuarios_" + timestamp + ".csv");
            exportarACsv(servicioRepository.findAll(), backupDirectory + "/servicios_" + timestamp + ".csv");
            
            System.out.println("✅ Backup completado: " + timestamp);
            limpiarBackupsAntiguos();
            
        } catch (Exception e) {
            System.err.println("❌ Error en backup automático: " + e.getMessage());
        }
    }
    
    public String realizarBackupManual() {
        try {
            realizarBackupAutomatico();
            return "Backup manual completado exitosamente";
        } catch (Exception e) {
            return "Error en backup manual: " + e.getMessage();
        }
    }
    
    private void crearDirectorioBackup() {
        File dir = new File(backupDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private void exportarACsv(List<?> datos, String filePath) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Exportado el: " + LocalDateTime.now());
            for (Object obj : datos) {
                writer.println(obj.toString().replace("\n", " ").replace(",", ";"));
            }
        }
    }
    
    private void limpiarBackupsAntiguos() {
        File dir = new File(backupDirectory);
        if (!dir.exists()) return;
        
        File[] files = dir.listFiles();
        long treintaDiasAtras = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        for (File file : files) {
            if (file.lastModified() < treintaDiasAtras) {
                file.delete();
                System.out.println("🗑️ Backup antiguo eliminado: " + file.getName());
            }
        }
    }
}