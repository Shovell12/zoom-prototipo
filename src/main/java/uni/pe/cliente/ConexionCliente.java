package uni.pe.cliente;

import com.google.gson.Gson;
import uni.pe.protocolo.MensajeSocket;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConexionCliente {
    private Socket socket;
    private PrintWriter salida;
    private final Gson gson = new Gson();

    // Lista de observadores (Ventanas) para el Patrón Observer
    private final List<MensajeListener> listeners = new ArrayList<>();

    // Diccionario de comandos para el Patrón Command
    @FunctionalInterface
    private interface ComandoCliente { void ejecutar(MensajeSocket msg); }
    private final Map<String, ComandoCliente> comandos = new HashMap<>();

    public ConexionCliente(String host, int puerto) {
        try {
            socket = new Socket(host, puerto);
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            registrarComandos();
            new Thread(this::escucharServidor).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registrarComandos() {
        // En lugar de un switch, mapeamos tipos de mensaje a acciones de notificación
        comandos.put(MensajeSocket.LOGIN_RESPONSE,       this::notificar);
        comandos.put(MensajeSocket.REGISTER_RESPONSE,    this::notificar);
        comandos.put(MensajeSocket.CREATE_ROOM_RESPONSE, this::notificar);
        comandos.put(MensajeSocket.WAITING_ROOM_UPDATE,  this::notificar);
        comandos.put(MensajeSocket.ADMIT_RESPONSE,       this::notificar);
        comandos.put(MensajeSocket.CHAT_MESSAGE,         this::notificar);
        comandos.put(MensajeSocket.FILE_NOTIFY,          this::notificar);
        comandos.put(MensajeSocket.CAMERA_FRAME,         this::notificar);
        comandos.put(MensajeSocket.AUDIO_FRAME,          this::notificar);
        comandos.put(MensajeSocket.ERROR,                this::notificar);
    }

    private void escucharServidor() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                procesarRespuesta(gson.fromJson(linea, MensajeSocket.class));
            }
        } catch (IOException e) {
            System.err.println("Conexión perdida con el servidor.");
        }
    }

    private void procesarRespuesta(MensajeSocket msg) {
        ComandoCliente comando = comandos.get(msg.getType());
        if (comando != null) {
            comando.ejecutar(msg);
        }
    }

    // Método puente para el Patrón Observer
    private void notificar(MensajeSocket msg) {
        for (MensajeListener listener : listeners) {
            listener.onMensajeRecibido(msg);
        }
    }

    public void agregarListener(MensajeListener l) {
        listeners.add(l);
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
            e.printStackTrace();
        }
    }
}