package mx.edu.utez.panaderia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Component
public class ConexionCelular {

    // Ruta del programa adb que instala Android Studio
    private final String adb = System.getenv("LOCALAPPDATA") + "\\Android\\Sdk\\platform-tools\\adb.exe";

    // Puerto del servidor (8080), leido de application.properties
    @Value("${server.port:8080}")
    private int puerto;

    // Celulares que ya estaban conectados (para avisar en consola solo cuando cambia algo)
    private Set<String> conectadosAntes = new HashSet<>();

    // @Scheduled: Spring ejecuta este metodo cada 5 segundos (5000 ms)
    @Scheduled(fixedDelay = 5000)
    public void mantenerConexion() {
        if (!new File(adb).exists()) {
            return; // esta computadora no tiene Android Studio: no hacemos nada
        }
        try {
            Set<String> conectados = new HashSet<>();
            // 1) "adb devices" lista los dispositivos conectados
            for (String linea : ejecutar(adb, "devices")) {
                String[] partes = linea.trim().split("\\s+");
                // Una linea valida se ve asi: "RZCY50HBWKJ    device"
                if (partes.length == 2 && partes[1].equals("device")) {
                    conectados.add(partes[0]);
                    // 2) A cada uno le activamos la conexion con el servidor
                    ejecutar(adb, "-s", partes[0], "reverse", "tcp:" + puerto, "tcp:" + puerto);
                }
            }
            // Aviso en la consola de IntelliJ cuando se conecta o desconecta un celular
            for (String id : conectados) {
                if (!conectadosAntes.contains(id)) System.out.println(">>> Celular conectado al servidor: " + id);
            }
            for (String id : conectadosAntes) {
                if (!conectados.contains(id)) System.out.println(">>> Celular desconectado: " + id);
            }
            conectadosAntes = conectados;
        } catch (Exception e) {
            // Si algo falla aqui no afecta al servidor; se vuelve a intentar en 5 segundos
        }
    }

    // Ejecuta un programa y regresa lo que escribio, linea por linea
    private List<String> ejecutar(String... comando) throws Exception {
        Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
        List<String> lineas = new BufferedReader(new InputStreamReader(proceso.getInputStream())).lines().toList();
        proceso.waitFor(5, TimeUnit.SECONDS);
        return lineas;
    }
}
