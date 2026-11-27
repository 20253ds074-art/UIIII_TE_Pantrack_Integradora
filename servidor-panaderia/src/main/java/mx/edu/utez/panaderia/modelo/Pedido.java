package mx.edu.utez.panaderia.modelo;

import jakarta.persistence.*;
import lombok.Data;

// Representa la tabla PEDIDO
@Entity
@Table(name = "pedido")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id del cliente (puede ser nulo si el trabajador escribio el nombre a mano)
    private Long clienteId;

    // Nombre del cliente o sucursal que se muestra en la lista
    private String clienteNombre;

    // Id del trabajador (repartidor) que lleva el pedido
    private Long trabajadorId;

    // Texto con los panes pedidos, por ejemplo: "Bolillo x10, Concha x5"
    // TEXT: texto largo, para que quepan pedidos con muchos tipos de pan
    // (un VARCHAR normal solo guarda 255 letras)
    @Column(columnDefinition = "TEXT")
    private String detalle;

    // Cantidad total de panes del pedido
    private Integer totalPiezas;

    // Precio total del pedido
    private Double total;

    // PENDIENTE o ENTREGADO
    private String estado;

    // Dinero que el trabajador ya le entrego al administrador.
    // La deuda del trabajador con el admin es: total - montoEntregado
    private Double montoEntregado;

    // Dinero que el cliente ya le pago al repartidor (lo registra el repartidor).
    // Lo que le falta pagar al cliente es: total - pagoCliente
    // DEFAULT 0: los pedidos que ya existian quedan con 0
    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double pagoCliente = 0.0;

    private String fecha;
}
