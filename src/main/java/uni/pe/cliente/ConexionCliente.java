package uni.pe.cliente;

import com.google.gson.Gson;
import uni.pe.protocolo.MensajeSocket;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ConexionCliente {

    private static final int PUERTO = 5000;

    private final String host;
    private Socket socket;
    private PrintWriter salida;
    private final Gson gson = new Gson();

    // Referencias a ventanas activas
    private VentanaLogin      ventanaLogin;
    private VentanaPrincipal  ventanaPrincipal;
    private VentanaReunion    ventanaReunion;

    private int    idUsuario;
    private String nombreUsuario;
    private String roomCode;
    private boolean esHost;

    private ByteArrayOutputStream bufferDescarga;
    private String nombreArchivoDescarga;

    public ConexionCliente(String host) {
        this.host = host;
    }

    public void iniciar() {
        try {
            socket = new Socket(host, PUERTO);
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            System.out.println("Conectado al servidor.");

            // Mostrar login
            SwingUtilities.invokeLater(() -> {
                ventanaLogin = new VentanaLogin(this);
                ventanaLogin.setVisible(true);
            });

            // Escuchar respuestas en hilo separado
            new Thread(this::escuchar).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar al servidor.\nVerifica que el servidor esté activo.",
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void escuchar() {
        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                MensajeSocket msg = gson.fromJson(linea, MensajeSocket.class);
                procesarRespuesta(msg);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                            "Conexión con el servidor perdida.",
                            "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void procesarRespuesta(MensajeSocket msg) {
        switch (msg.getType()) {
            case MensajeSocket.LOGIN_RESPONSE      -> handleLoginResp(msg);
            case MensajeSocket.REGISTER_RESPONSE   -> handleRegistroResp(msg);
            case MensajeSocket.CREATE_ROOM_RESPONSE-> handleCrearSalaResp(msg);
            case MensajeSocket.WAITING_ROOM_UPDATE -> handleEspera(msg);
            case MensajeSocket.ADMIT_RESPONSE      -> handleAdmitResp(msg);
            case MensajeSocket.CHAT_MESSAGE        -> handleChat(msg);
            case MensajeSocket.FILE_NOTIFY         -> handleFileNotif(msg);
            case MensajeSocket.FILE_START          -> iniciarDescarga(msg);
            case MensajeSocket.FILE_CHUNK          -> recibirChunkDescarga(msg);
            case MensajeSocket.FILE_END            -> finalizarDescarga();
            case MensajeSocket.CAMERA_FRAME        -> handleCamara(msg);
            case MensajeSocket.ERROR               -> mostrarError(msg.getMensaje());
        }
    }

    // ── HANDLERS ──────────────────────────────────────────────────────────────

    private void handleLoginResp(MensajeSocket msg) {
        if (msg.isExito()) {
            this.idUsuario    = msg.getIdUsuario();
            this.nombreUsuario = msg.getNombreUsuario();
            SwingUtilities.invokeLater(() -> {
                ventanaLogin.setVisible(false);
                ventanaPrincipal = new VentanaPrincipal(this, idUsuario, nombreUsuario);
                ventanaPrincipal.setVisible(true);
            });
        } else {
            ventanaLogin.mostrarMensaje(msg.getMensaje(), true);
        }
    }

    private void handleRegistroResp(MensajeSocket msg) {
        ventanaLogin.mostrarMensaje(msg.getMensaje(), !msg.isExito());
    }

    private void handleCrearSalaResp(MensajeSocket msg) {
        if (msg.isExito()) {
            this.roomCode = msg.getRoomCode();
            this.esHost   = true;
            SwingUtilities.invokeLater(() -> {
                ventanaPrincipal.setVisible(false);
                ventanaReunion = new VentanaReunion(
                        this, idUsuario, nombreUsuario, roomCode, true);
                ventanaReunion.setVisible(true);
            });
        } else {
            mostrarError(msg.getMensaje());
        }
    }

    private void handleEspera(MensajeSocket msg) {
        if (msg.isExito() == false && msg.getMensaje() != null) {
            // Soy el invitado esperando
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(ventanaPrincipal,
                            msg.getMensaje(), "Sala de espera", JOptionPane.INFORMATION_MESSAGE));
        } else if (ventanaReunion != null) {
            // Soy el host, llega un usuario en espera
            ventanaReunion.agregarAEspera(msg.getIdUsuario(), msg.getNombreUsuario());
        }
    }

    private void handleAdmitResp(MensajeSocket msg) {
        if (msg.isAceptado()) {
            this.roomCode = msg.getRoomCode();
            this.esHost   = false;
            SwingUtilities.invokeLater(() -> {
                if (ventanaPrincipal != null) ventanaPrincipal.setVisible(false);
                ventanaReunion = new VentanaReunion(
                        this, idUsuario, nombreUsuario, roomCode, false);
                ventanaReunion.setVisible(true);
            });
        } else {
            mostrarError(msg.getMensaje());
        }
    }

    private void handleChat(MensajeSocket msg) {
        if (ventanaReunion != null)
            ventanaReunion.agregarMensajeChat(msg.getNombreUsuario() + ": " + msg.getContenido());
    }

    private void handleFileNotif(MensajeSocket msg) {
        if (ventanaReunion != null)
            ventanaReunion.agregarArchivo(msg.getNombreArchivo());
    }

    private void iniciarDescarga(MensajeSocket msg) {
        bufferDescarga = new ByteArrayOutputStream();
        nombreArchivoDescarga = msg.getNombreArchivo();
    }

    private void recibirChunkDescarga(MensajeSocket msg) {
        if (bufferDescarga != null) {
            byte[] datos = Base64.getDecoder().decode(msg.getChunkBase64());
            bufferDescarga.write(datos, 0, datos.length);
        }
    }

    private void finalizarDescarga() {
        if (bufferDescarga == null) return;
        byte[] datos = bufferDescarga.toByteArray();
        String nombre = nombreArchivoDescarga;
        bufferDescarga = null;
        nombreArchivoDescarga = null;
        SwingUtilities.invokeLater(() -> {
            if (ventanaReunion != null) ventanaReunion.guardarDescarga(nombre, datos);
        });
    }

    private void handleCamara(MensajeSocket msg) {
        if (ventanaReunion != null)
            ventanaReunion.mostrarFrameRemoto(msg.getFrameBase64());
    }

    private void mostrarError(String texto) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, texto, "Error", JOptionPane.ERROR_MESSAGE));
    }

    public void enviar(MensajeSocket msg) {
        if (salida != null) salida.println(gson.toJson(msg));
    }

    // ── MAIN DEL CLIENTE ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        String ip = args.length > 0 ? args[0] : "localhost";
        SwingUtilities.invokeLater(() -> new ConexionCliente(ip).iniciar());
    }
}