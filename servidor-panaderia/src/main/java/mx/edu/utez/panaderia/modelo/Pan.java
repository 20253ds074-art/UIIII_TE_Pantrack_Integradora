package mx.edu.utez.panaderia.modelo;

import jakarta.persistence.*;
import lombok.Data;

// @Entity indica que esta clase representa la tabla PAN de la base de datos
@Entity
@Table(name = "pan")
@Data // Lombok crea los getters y setters
public class Pan {

    // Llave primaria; Oracle genera el numero automaticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private Double precio;

    // Foto tomada con la camara, guardada como texto Base64 (columna CLOB)
    @Lob
    private String foto;
}
