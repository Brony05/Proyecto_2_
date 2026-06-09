package mx.com.ubam.isc.apijwt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import mx.com.ubam.isc.apijwt.model.Cliente;
import mx.com.ubam.isc.apijwt.model.Cita;
import mx.com.ubam.isc.apijwt.model.Usuario;
import mx.com.ubam.isc.apijwt.repository.CitaRepository;
import mx.com.ubam.isc.apijwt.repository.ClienteRepository;
import mx.com.ubam.isc.apijwt.repository.UsuarioRepository;
import mx.com.ubam.isc.apijwt.service.ClienteService;
import jakarta.transaction.Transactional;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;
    
    @Autowired
    private CitaRepository citaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtener(@PathVariable Long id) {
        return clienteService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Cliente crear(@RequestBody Cliente cliente) {
        return clienteService.crear(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @RequestBody Cliente cliente) {
        cliente.setId(id);
        return ResponseEntity.ok(clienteService.actualizar(cliente));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            System.out.println("🔵 Eliminando cliente con ID: " + id);
            
            // Buscar el cliente
            Cliente cliente = clienteService.obtenerPorId(id).orElse(null);
            if (cliente == null) {
                return ResponseEntity.notFound().build();
            }
            
            System.out.println("Cliente encontrado: " + cliente.getNombre() + " - " + cliente.getCorreo());
            
            // 1. Eliminar todas las citas del cliente
            List<Cita> citas = citaRepository.findByClienteId(id);
            if (!citas.isEmpty()) {
                citaRepository.deleteAll(citas);
                System.out.println("✅ Eliminadas " + citas.size() + " citas del cliente");
            }
            
            // 2. Buscar y eliminar el usuario asociado (si es paciente)
            String correoCliente = cliente.getCorreo();
            if (correoCliente != null && !correoCliente.isEmpty()) {
                Usuario usuario = usuarioRepository.findByUsername(correoCliente).orElse(null);
                if (usuario != null && "PACIENTE".equals(usuario.getRol())) {
                    usuarioRepository.delete(usuario);
                    System.out.println("✅ Usuario eliminado: " + usuario.getUsername());
                }
            }
            
            // 3. Eliminar el cliente
            clienteService.eliminar(id);
            System.out.println("✅ Cliente eliminado exitosamente");
            
            return ResponseEntity.ok("Cliente eliminado correctamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error al eliminar cliente: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al eliminar cliente: " + e.getMessage());
        }
    }

    @GetMapping("/buscar")
    public List<Cliente> buscar(@RequestParam String nombre) {
        return clienteService.buscarPorNombre(nombre);
    }
}