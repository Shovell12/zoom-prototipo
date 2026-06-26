package uni.pe.cliente;

import com.google.gson.Gson;
import uni.pe.protocolo.MensajeSocket;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConexionCliente {
    private Socket socket;
    private PrintWriter salida;
    private final Gson gson = new Gson();

    // Observadores (patrón Observer): cada ventana registrada recibe todos los mensajes
    // y decide en su propio switch qué tipos le interesan.
    private final List<MensajeListener> listeners = new CopyOnWriteArrayList<>();

    public ConexionCliente(String host, int puerto) {
        try {
            socket = new Socket(host, puerto);
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Thread hiloLector = new Thread(this::escucharServidor);
            hiloLector.setDaemon(true);
            hiloLector.start();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo conectar al servidor " + host + ":" + puerto, e);
        }
    }

    private void escucharServidor() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                notificar(gson.fromJson(linea, MensajeSocket.class));
            }
        } catch (IOException e) {
            System.err.println("Conexión perdida con el servidor.");
        }
        // Notifica caída de conexión si el servidor no envió ROOM_CLOSED
        notificar(new MensajeSocket.Builder(MensajeSocket.ROOM_CLOSED)
                .respuesta(true, "Conexión perdida con el servidor.")
                .build());
    }

    private void notificar(MensajeSocket msg) {
        for (MensajeListener listener : listeners) {
            listener.onMensajeRecibido(msg);
        }
    }

    public void agregarListener(MensajeListener l) {
        listeners.add(l);
    }

    public void removerListener(MensajeListener l) {
        listeners.remove(l);
    }

    public void enviar(MensajeSocket msg) {
        if (salida != null) {
            salida.println(gson.toJson(msg));
        }
    }

    public void cerrar() {
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
}