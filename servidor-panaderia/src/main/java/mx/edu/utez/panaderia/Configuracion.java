package mx.edu.utez.panaderia;

import mx.edu.utez.panaderia.modelo.Pan;
import mx.edu.utez.panaderia.modelo.Usuario;
import mx.edu.utez.panaderia.repositorio.PanRepository;
import mx.edu.utez.panaderia.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Configuracion general del servidor
@Configuration
public class Configuracion {

    // Objeto que cifra y compara contrasenas con BCrypt.
    // @Bean hace que Spring lo cree una vez y lo pase a quien lo necesite.
    @Bean
    public BCryptPasswordEncoder cifrador() {
        return new BCryptPasswordEncoder();
    }

    // Se ejecuta una vez al arrancar el servidor.
    // Si la base de datos esta vacia, crea el administrador y unos panes de ejemplo.
    @Bean
    public CommandLineRunner datosIniciales(UsuarioRepository usuarios, PanRepository panes,
                                            BCryptPasswordEncoder cifrador) {
        return args -> {
            if (usuarios.countByRol("ADMIN") == 0) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setCorreo("admin@panaderia.com");
                admin.setContrasena(cifrador.encode("admin123")); // se guarda cifrada
                admin.setRol("ADMIN");
                usuarios.save(admin);
            }
            if (panes.count() == 0) {
                panes.save(nuevoPan("Bolillo", 4.50));
                panes.save(nuevoPan("Concha", 12.00));
                panes.save(nuevoPan("Cuerno", 16.00));
            }
        };
    }

    private Pan nuevoPan(String nombre, double precio) {
        Pan pan = new Pan();
        pan.setNombre(nombre);
        pan.setPrecio(precio);
        return pan;
    }
}
