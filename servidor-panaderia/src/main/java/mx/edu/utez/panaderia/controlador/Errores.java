package mx.edu.utez.panaderia.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

// Arma las respuestas de error con un mensaje en JSON: {"mensaje": "..."}
// La app lee ese mensaje y se lo muestra al usuario tal cual.
public class Errores {

    public static ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("mensaje", mensaje));
    }
}
