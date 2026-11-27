package mx.edu.utez.panaderia.modelo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

// Representa la tabla USUARIO. Guarda a los tres roles: ADMIN, TRABAJADOR y CLIENTE.
@Entity
@Table(name = "usuario")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre de la persona (en el cliente es el nombre del encargado)
    private String nombre;

    private String apellidos;

    // Solo para clientes: id del repartidor que lo atiende.
    // null = el cliente se registro solo y su solicitud espera a que el admin le asigne repartidor
    private Long repartidorId;

    // unique = true: no puede haber dos usuarios con el mismo correo
    @Column(unique = true)
    private String correo;

    // Se guarda CIFRADA con BCrypt (nunca el texto original).
    // WRITE_ONLY: la app puede mandarla, pero la API nunca la regresa en sus respuestas.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;

    // ADMIN, TRABAJADOR o CLIENTE
    private String rol;

    // Solo para trabajadores: numero de nomina o NSS
    private String nss;

    // Solo para clientes
    private String sucursal;
    private String localizacion;

    // Solo para trabajadores: foto tomada con la camara
    @Lob
    private String foto;
}
