package mx.edu.utez.panaderia.controlador;

import mx.edu.utez.panaderia.modelo.Pedido;
import mx.edu.utez.panaderia.modelo.Usuario;
import mx.edu.utez.panaderia.repositorio.PedidoRepository;
import mx.edu.utez.panaderia.repositorio.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// Maneja el login, el CRUD de usuarios (trabajadores y clientes)
// y la asignacion de clientes a repartidores
@RestController
@RequestMapping("/api")
public class UsuarioController {

    // Contrasena segura: minimo 10 caracteres, con mayuscula, minuscula, numero y un caracter especial
    private static final String REGLA_CONTRASENA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,}$";

    private final UsuarioRepository repositorio;
    private final PedidoRepository pedidos;
    private final BCryptPasswordEncoder cifrador;

    public UsuarioController(UsuarioRepository repositorio, PedidoRepository pedidos, BCryptPasswordEncoder cifrador) {
        this.repositorio = repositorio;
        this.pedidos = pedidos;
        this.cifrador = cifrador;
    }

    // LOGIN: POST /api/login  con {"correo": "...", "contrasena": "..."}
    // Buscamos al usuario por correo y comparamos la contrasena escrita con la cifrada.
    // Si no coincide regresamos 401 (no autorizado) sin decir cual dato estuvo mal.
    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody Usuario datos) {
        if (datos.getCorreo() == null || datos.getCorreo().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<Usuario> encontrado = repositorio.findByCorreo(datos.getCorreo());
        if (encontrado.isPresent()
                && encontrado.get().getContrasena() != null
                && datos.getContrasena() != null
                && cifrador.matches(datos.getContrasena(), encontrado.get().getContrasena())) {
            return ResponseEntity.ok(encontrado.get());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    @GetMapping("/usuarios")
    public List<Usuario> obtenerPorRol(@RequestParam String rol,
                                       @RequestParam(required = false) Long repartidorId) {
        if (repartidorId != null) {
            return repositorio.findByRolAndRepartidorId(rol, repartidorId);
        }
        return repositorio.findByRol(rol);
    }

    // READ: GET /api/usuarios/solicitudes -> clientes que se registraron solos y esperan repartidor
    @GetMapping("/usuarios/solicitudes")
    public List<Usuario> obtenerSolicitudes() {
        return repositorio.findByRolAndRepartidorIdIsNull("CLIENTE");
    }

    // READ: GET /api/usuarios/5
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE: POST /api/usuarios  (registro de cliente o alta de trabajador)
    @PostMapping("/usuarios")
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        // Seguridad: desde la app nadie puede crear otro administrador
        if (!"TRABAJADOR".equals(usuario.getRol()) && !"CLIENTE".equals(usuario.getRol())) {
            return Errores.respuesta(HttpStatus.FORBIDDEN, "No se permite crear ese tipo de usuario");
        }
        String error = validar(usuario, true);
        if (error != null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, error); // 400 = datos invalidos
        }
        // No se permiten dos usuarios con el mismo correo ni dos trabajadores con la misma nomina (409)
        if (usuario.getCorreo() != null && repositorio.findByCorreo(usuario.getCorreo()).isPresent()) {
            return Errores.respuesta(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }
        if (usuario.getNss() != null && repositorio.findByNss(usuario.getNss()).isPresent()) {
            return Errores.respuesta(HttpStatus.CONFLICT, "Ese numero de nomina ya esta registrado");
        }
        usuario.setId(null);
        // Un trabajador no tiene repartidor
        if ("TRABAJADOR".equals(usuario.getRol())) {
            usuario.setRepartidorId(null);
        }
        // Ciframos la contrasena antes de guardarla
        if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {
            usuario.setContrasena(cifrador.encode(usuario.getContrasena()));
        } else {
            usuario.setContrasena(null);
        }
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    // UPDATE: PUT /api/usuarios/5  (editar trabajador, cliente o el perfil propio)
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Usuario usuario) {
        Optional<Usuario> existente = repositorio.findById(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Seguridad: el rol no se puede cambiar al editar (nadie se puede volver ADMIN)
        usuario.setRol(existente.get().getRol());
        // El repartidor asignado no cambia al editar los datos (solo el admin lo cambia aparte)
        usuario.setRepartidorId(existente.get().getRepartidorId());
        String error = validar(usuario, false);
        if (error != null) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, error);
        }
        // Si el correo o la nomina ya los usa OTRO usuario, no se permite.
        // Solo se busca si el dato viene lleno (buscar "vacio" encontraria a muchos usuarios)
        if (usuario.getCorreo() != null) {
            Optional<Usuario> mismoCorreo = repositorio.findByCorreo(usuario.getCorreo());
            if (mismoCorreo.isPresent() && !mismoCorreo.get().getId().equals(id)) {
                return Errores.respuesta(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
            }
        }
        if (usuario.getNss() != null) {
            Optional<Usuario> mismaNomina = repositorio.findByNss(usuario.getNss());
            if (mismaNomina.isPresent() && !mismaNomina.get().getId().equals(id)) {
                return Errores.respuesta(HttpStatus.CONFLICT, "Ese numero de nomina ya esta registrado");
            }
        }
        usuario.setId(id);
        // Si no escribio contrasena nueva, se queda la que ya tenia
        if (usuario.getContrasena() == null || usuario.getContrasena().isBlank()) {
            usuario.setContrasena(existente.get().getContrasena());
        } else {
            usuario.setContrasena(cifrador.encode(usuario.getContrasena()));
        }
        return ResponseEntity.ok(repositorio.save(usuario));
    }

    // PUT /api/usuarios/5/repartidor?repartidorId=2
    // El admin asigna (o cambia) el repartidor de un cliente.
    // Los pedidos de ese cliente que no tenian repartidor pasan a ese repartidor.
    @PutMapping("/usuarios/{id}/repartidor")
    public ResponseEntity<?> asignarRepartidor(@PathVariable Long id, @RequestParam Long repartidorId) {
        Optional<Usuario> cliente = repositorio.findById(id);
        Optional<Usuario> repartidor = repositorio.findById(repartidorId);
        if (cliente.isEmpty() || !"CLIENTE".equals(cliente.get().getRol())) {
            return Errores.respuesta(HttpStatus.NOT_FOUND, "No se encontro el cliente");
        }
        if (repartidor.isEmpty() || !"TRABAJADOR".equals(repartidor.get().getRol())) {
            return Errores.respuesta(HttpStatus.BAD_REQUEST, "Elige un repartidor valido");
        }
        cliente.get().setRepartidorId(repartidorId);
        for (Pedido p : pedidos.findByClienteId(id)) {
            if (p.getTrabajadorId() == null) {
                p.setTrabajadorId(repartidorId);
                pedidos.save(p);
            }
        }
        return ResponseEntity.ok(repositorio.save(cliente.get()));
    }

    // DELETE: DELETE /api/usuarios/5
    // Si se borra un repartidor, sus clientes regresan a "solicitudes" para asignarles otro
    @DeleteMapping("/usuarios/{id}")
    public void eliminar(@PathVariable Long id) {
        for (Usuario c : repositorio.findByRolAndRepartidorId("CLIENTE", id)) {
            c.setRepartidorId(null);
            repositorio.save(c);
        }
        repositorio.deleteById(id);
    }

    // Revisa los datos. Regresa el mensaje del error, o null si todo esta bien.
    private String validar(Usuario u, boolean esNuevo) {
        if (u.getNombre() == null || u.getNombre().isBlank()) {
            return "El nombre es obligatorio";
        }
        if (u.getApellidos() == null || u.getApellidos().isBlank()) {
            return "Los apellidos son obligatorios";
        }
        if ("TRABAJADOR".equals(u.getRol())) {
            // Numero de nomina: exactamente 6 numeros, sin letras, espacios ni simbolos
            if (u.getNss() == null || !u.getNss().matches("\\d{6}")) {
                return "El numero de nomina debe tener exactamente 6 numeros";
            }
            if (u.getCorreo() == null || u.getCorreo().isBlank()) {
                return "El correo es obligatorio para un trabajador";
            }
        }
        boolean traeContrasena = u.getContrasena() != null && !u.getContrasena().isBlank();
        // Al crear una cuenta con correo, la contrasena es obligatoria
        if (esNuevo && u.getCorreo() != null && !traeContrasena) {
            return "La contrasena es obligatoria";
        }
        if (traeContrasena && !u.getContrasena().matches(REGLA_CONTRASENA)) {
            return "La contrasena debe tener minimo 10 caracteres, una mayuscula, una minuscula, un numero y un caracter especial";
        }
        return null;
    }
}
