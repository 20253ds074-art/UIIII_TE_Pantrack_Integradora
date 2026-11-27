package mx.edu.utez.panaderia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Clase principal: al ejecutarla se levanta el servidor en el puerto 8080
// @EnableScheduling: permite tareas que se repiten solas (ver ConexionCelular)
@SpringBootApplication
@EnableScheduling
public class PanaderiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(PanaderiaApplication.class, args);
	}
}
