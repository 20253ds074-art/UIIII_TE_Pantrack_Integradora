package mx.edu.utez.panaderia.repositorio;

import mx.edu.utez.panaderia.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring crea la consulta solo con leer el nombre del metodo:
    // SELECT * FROM usuario WHERE rol = ?
    List<Usuario> findByRol(String rol);

    // Clientes de un repartidor: WHERE rol = ? AND repartidor_id = ?
    List<Usuario> findByRolAndRepartidorId(String rol, Long repartidorId);

    // Solicitudes: clientes que todavia no tienen repartidor (repartidor_id IS NULL)
    List<Usuario> findByRolAndRepartidorIdIsNull(String rol);

    // SELECT * FROM usuario WHERE correo = ?  (para el login y para no repetir correos)
    Optional<Usuario> findByCorreo(String correo);

    // Para no repetir numeros de nomina
    Optional<Usuario> findByNss(String nss);

    // Cuenta cuantos usuarios hay con un rol (para saber si ya existe el administrador)
    long countByRol(String rol);
}
