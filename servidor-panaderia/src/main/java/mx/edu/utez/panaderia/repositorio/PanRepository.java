package mx.edu.utez.panaderia.repositorio;

import mx.edu.utez.panaderia.modelo.Pan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository ya trae los metodos del CRUD: findAll, findById, save y deleteById
public interface PanRepository extends JpaRepository<Pan, Long> {

    // Busca un pan por nombre sin importar mayusculas o minusculas
    // (para no permitir dos panes llamados "Concha" y "concha")
    Optional<Pan> findByNombreIgnoreCase(String nombre);
}
