package mx.edu.utez.panaderia.controlador;

import mx.edu.utez.panaderia.modelo.Pedido;
import mx.edu.utez.panaderia.modelo.Usuario;
import mx.edu.utez.panaderia.repositorio.PedidoRepository;
import mx.edu.utez.panaderia.repositorio.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    // Maximo de pedidos que un cliente puede hacer en un dia
    private static final int MAXIMO_POR_DIA = 2;

    private final PedidoRepository repositorio;
    private final UsuarioRepository usuarios;

    public PedidoController(PedidoRepository repositorio, UsuarioRepository usuarios) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
    }


    @GetMapping
    public List<Pedido> obtener(@RequestParam(required = false) Long clienteId,
                                @RequestParam(required = false) Long trabajadorId) {
        if (clienteId != null) {
            return repositorio.findByClienteId(clienteId);
        }
        if (trabajadorId != null) {
            return repositorio.findByTrabajadorId(trabajadorId);
        }
        return repositorio.findAll();
    }

    // READ: GET /api/pedidos/5
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE: POST /api/pedidos
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Pedido pedido) {
        String error = validar(pedido);
        if (error != null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, error);
        }
        // La fecha la pone el servidor para que nadie la pueda cambiar
        String hoy = LocalDate.now().toString();
        pedido.setFecha(hoy);

        if (pedido.getClienteId() != null) {
            // Un cliente no puede tener más de 2 pedidos en el mismo día
            if (repositorio.countByClienteIdAndFecha(pedido.getClienteId(), hoy) >= MAXIMO_POR_DIA) {
                return Errores.respuesta(HttpStatus.BAD_REQUEST,
                        "Este cliente ya tiene 2 pedidos hoy. Solo se permiten 2 pedidos por dia");
            }
            // El pedido se asigna solo al repartidor del cliente (ya no hay que elegirlo)
            Optional<Usuario> cliente = usuarios.findById(pedido.getClienteId());
            if (cliente.isPresent() && cliente.get().getRepartidorId() != null) {
                pedido.setTrabajadorId(cliente.get().getRepartidorId());
            }
        }
        // Todo pedido nuevo empieza pendiente y sin pagos
        pedido.setId(null);
        pedido.setEstado("PENDIENTE");
        pedido.setMontoEntregado(0.0);
        pedido.setPagoCliente(0.0);
        return ResponseEntity.ok(repositorio.save(pedido));
    }

    // UPDATE: PUT /api/pedidos/5  (estado, repartidor, pago del cliente o dinero entregado al admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Pedido pedido) {
        String error = validar(pedido);
        if (error != null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, error);
        }
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // No se puede entregar un pedido que nadie lleva
        if ("ENTREGADO".equals(pedido.getEstado()) && pedido.getTrabajadorId() == null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST,
                    "Asigna un repartidor antes de marcar el pedido como entregado");
        }
        pedido.setId(id);
        return ResponseEntity.ok(repositorio.save(pedido));
    }

    // DELETE: DELETE /api/pedidos/5  (cancelar pedido)
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        repositorio.deleteById(id);
    }

    // Revisa los datos. Regresa el mensaje del error, o null si todo esta bien.
    private String validar(Pedido p) {
        if (p.getClienteNombre() == null || p.getClienteNombre().isBlank()) {
            return "Falta el nombre del cliente";
        }
        if (p.getTotalPiezas() == null || p.getTotalPiezas() <= 0 || p.getTotal() == null) {
            return "El pedido debe tener al menos un pan";
        }
        double entregado = p.getMontoEntregado() == null ? 0 : p.getMontoEntregado();
        double pagado = p.getPagoCliente() == null ? 0 : p.getPagoCliente();
        // 0.001 = margen por los decimales
        if (entregado < 0 || entregado > p.getTotal() + 0.001) {
            return "El dinero entregado debe estar entre 0 y el total del pedido";
        }
        if (pagado < 0 || pagado > p.getTotal() + 0.001) {
            return "El pago del cliente debe estar entre 0 y el total del pedido";
        }
        return null;
    }
}
