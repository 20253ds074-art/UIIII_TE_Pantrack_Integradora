package mx.edu.utez.panaderia.controlador;

import mx.edu.utez.panaderia.modelo.Pan;
import mx.edu.utez.panaderia.repositorio.PanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// @RestController: cada metodo responde con JSON
// @RequestMapping: todas las rutas de esta clase empiezan con /api/panes
@RestController
@RequestMapping("/api/panes")
public class PanController {

    private final PanRepository repositorio;

    // Spring nos pasa el repositorio automaticamente (inyeccion de dependencias)
    public PanController(PanRepository repositorio) {
        this.repositorio = repositorio;
    }

    // READ: GET /api/panes  -> lista de todos los panes
    @GetMapping
    public List<Pan> obtenerTodos() {
        return repositorio.findAll();
    }

    // READ: GET /api/panes/5  -> un solo pan
    @GetMapping("/{id}")
    public ResponseEntity<Pan> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)                      // si existe responde 200 con el pan
                .orElse(ResponseEntity.notFound().build());   // si no existe responde 404
    }

    // CREATE: POST /api/panes  (el pan viene en el cuerpo como JSON)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Pan pan) {
        // 400 = datos invalidos (sin nombre o precio menor al minimo)
        String error = validar(pan);
        if (error != null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, error);
        }
        pan.setNombre(pan.getNombre().trim());
        // 409 = ya existe un pan con ese nombre
        if (repositorio.findByNombreIgnoreCase(pan.getNombre()).isPresent()) {
            return Errores.respuesta(HttpStatus.CONFLICT, "Ya existe un pan con ese nombre");
        }
        pan.setId(null); // para que MySQL genere un id nuevo
        return ResponseEntity.ok(repositorio.save(pan));
    }

    // UPDATE: PUT /api/panes/5
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Pan pan) {
        String error = validar(pan);
        if (error != null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, error);
        }
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        pan.setNombre(pan.getNombre().trim());
        // Si el nombre ya lo tiene OTRO pan, no se permite
        Optional<Pan> mismoNombre = repositorio.findByNombreIgnoreCase(pan.getNombre());
        if (mismoNombre.isPresent() && !mismoNombre.get().getId().equals(id)) {
            return Errores.respuesta(HttpStatus.CONFLICT, "Ya existe un pan con ese nombre");
        }
        pan.setId(id); // al tener id, save() actualiza en lugar de crear
        return ResponseEntity.ok(repositorio.save(pan));
    }

    // DELETE: DELETE /api/panes/5
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        repositorio.deleteById(id);
    }

    // Precio minimo de un pan: $2.50 (lo que cuesta un bolillo o una telera)
    private static final double PRECIO_MINIMO = 2.50;

    // Revisa los datos. Regresa el mensaje del error, o null si todo esta bien.
    private String validar(Pan pan) {
        if (pan.getNombre() == null || pan.getNombre().isBlank()) {
            return "Escribe el nombre del pan";
        }
        if (pan.getPrecio() == null || pan.getPrecio() < PRECIO_MINIMO) {
            return "El precio minimo de un pan es $2.50";
        }
        return null;
    }
}
