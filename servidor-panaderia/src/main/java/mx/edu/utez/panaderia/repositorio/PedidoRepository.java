package mx.edu.utez.panaderia.repositorio;

import mx.edu.utez.panaderia.modelo.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Pedidos de un cliente (para "Mis pedidos" del cliente)
    List<Pedido> findByClienteId(Long clienteId);

    // Pedidos de un trabajador (para "Mis pedidos" del repartidor)
    List<Pedido> findByTrabajadorId(Long trabajadorId);

    // Cuantos pedidos tiene un cliente en una fecha (maximo 2 por dia)
    long countByClienteIdAndFecha(Long clienteId, String fecha);
}
