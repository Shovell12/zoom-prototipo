package uni.pe.servidor;

import com.google.gson.Gson;
import uni.pe.basedatos.*;
import uni.pe.modelo.Usuario;
import uni.pe.modelo.Sala;
import uni.pe.protocolo.MensajeSocket;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ManejadorCliente implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();
    private PrintWriter salida;
    private int idUsuario;
    private String nombreUsuario;
    private String roomCode;

    @FunctionalInterface
    private interface Comando { void ejecutar(MensajeSocket msg); }
    private final Map<String, Comando> comandos = new HashMap<>();

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
        registrarComandos();
    }

    private void registrarComandos() {
        comandos.put(MensajeSocket.LOGIN_REQUEST, this::handleLogin);
        comandos.put(MensajeSocket.REGISTER_REQUEST, this::handleRegistro);
        comandos.put(MensajeSocket.CREATE_ROOM, this::handleCrearSala);
        comandos.put(MensajeSocket.JOIN_ROOM_REQUEST, this::handleUnirSala);
        comandos.put(MensajeSocket.ADMIT_USER, this::handleAdmitir);
        comandos.put(MensajeSocket.CHAT_MESSAGE, this::handleChat);
        comandos.put(MensajeSocket.CAMERA_FRAME, this::handleCamara);
        comandos.put(MensajeSocket.CAMERA_STOP,  this::handleCamaraStop);
        comandos.put(MensajeSocket.AUDIO_FRAME, this::handleAudio);
        comandos.put(MensajeSocket.LEAVE_ROOM,  this::handleSalir);
        comandos.put(MensajeSocket.FILE_START,        this::handleFileStart);
        comandos.put(MensajeSocket.FILE_CHUNK,        this::handleFileChunk);
        comandos.put(MensajeSocket.FILE_END,          this::handleFileEnd);
        comandos.put(MensajeSocket.SCREEN_SHARE,      this::handleScreenShare);
        comandos.put(MensajeSocket.SCREEN_SHARE_STOP, this::handleScreenShareStop);
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            String linea;
            while ((linea = entrada.readLine()) != null) {
                MensajeSocket msg = gson.fromJson(linea, MensajeSocket.class);
                Comando cmd = comandos.get(msg.getType());
                if (cmd != null) cmd.ejecutar(msg);
            }
        } catch (IOException e) { System.out.println("Cliente desconectado: " + nombreUsuario); }
        finally { desconectar(); }
    }

    // --- LOGICA DE HANDLERS ---

    private void handleLogin(MensajeSocket msg) {
        Usuario u = UsuarioDAO.login(msg.getCorreo(), msg.getPassword());
        MensajeSocket resp = new MensajeSocket.Builder(MensajeSocket.LOGIN_RESPONSE)
                .respuesta(u != null, u != null ? "Bienvenido, " + u.getNombres() : "Error en credenciales")
                .build();
        if (u != null) {
            this.idUsuario = u.getIdUsuario();
            this.nombreUsuario = u.getNombres();
            Servidor.registrarCliente(idUsuario, this);
            resp.setIdUsuario(u.getIdUsuario());
            resp.setNombreUsuario(u.getNombres());
        }
        enviar(resp);
    }

    private void handleRegistro(MensajeSocket msg) {
        boolean ok = UsuarioDAO.registrar(msg.getNombres(), msg.getCorreo(), msg.getPassword());
        enviar(new MensajeSocket.Builder(MensajeSocket.REGISTER_RESPONSE)
                .respuesta(ok, ok ? "Registro exitoso" : "El correo ya existe")
                .build());
    }

    private void handleCrearSala(MensajeSocket msg) {
        String codigo = msg.getRoomCode() != null ? msg.getRoomCode() : "SALA" + (int)(Math.random() * 9000 + 1000);
        Sala sala = SalaDAO.crear(codigo, msg.getRoomName(), idUsuario);
        if (sala != null) {
            this.roomCode = codigo;
            SalaDAO.admitirUsuario(sala.getIdSala(), idUsuario);
            Servidor.unirseASala(codigo, idUsuario);
            enviar(new MensajeSocket.Builder(MensajeSocket.CREATE_ROOM_RESPONSE).sala(codigo, msg.getRoomName()).respuesta(true, "Sala creada").build());
        } else {
            enviar(new MensajeSocket.Builder(MensajeSocket.CREATE_ROOM_RESPONSE).respuesta(false, "Error al crear sala").build());
        }
    }

    private void handleUnirSala(MensajeSocket msg) {
        Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
        if (sala == null) { enviar(error("Sala no encontrada")); return; }
        this.roomCode = msg.getRoomCode();
        SalaDAO.registrarSolicitud(sala.getIdSala(), idUsuario);
        Servidor.enviarA(sala.getIdHost(), new MensajeSocket.Builder(MensajeSocket.WAITING_ROOM_UPDATE)
                .usuario(idUsuario, nombreUsuario)
                .sala(roomCode)
                .build());
    }

    private void handleAdmitir(MensajeSocket msg) {
        Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
        if (sala == null) return;
        if (msg.isAceptado()) {
            SalaDAO.admitirUsuario(sala.getIdSala(), msg.getIdUsuario());
            String nombreNuevo = msg.getNombreUsuario();
            // Enviar participantes existentes al nuevo usuario antes de añadirlo
            for (Map.Entry<Integer, String> e : Servidor.getParticipantes(msg.getRoomCode()).entrySet()) {
                Servidor.enviarA(msg.getIdUsuario(), new MensajeSocket.Builder(MensajeSocket.USER_JOINED)
                        .sala(msg.getRoomCode())
                        .usuario(e.getKey(), e.getValue())
                        .build());
            }
            // Añadir nuevo usuario a la sala
            Servidor.unirseASala(msg.getRoomCode(), msg.getIdUsuario());
            // Notificar a los existentes sobre el nuevo participante
            Servidor.broadcast(msg.getRoomCode(), new MensajeSocket.Builder(MensajeSocket.USER_JOINED)
                    .sala(msg.getRoomCode())
                    .usuario(msg.getIdUsuario(), nombreNuevo)
                    .build(), msg.getIdUsuario());
        }
        MensajeSocket resp = new MensajeSocket.Builder(MensajeSocket.ADMIT_RESPONSE)
                .sala(msg.getRoomCode())
                .build();
        resp.setAceptado(msg.isAceptado());
        Servidor.enviarA(msg.getIdUsuario(), resp);
    }

    private void handleChat(MensajeSocket msg) {
        Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
        if (sala != null) MensajeDAO.guardar(sala.getIdSala(), idUsuario, msg.getContenido());
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleCamara(MensajeSocket msg) {
        msg.setIdUsuario(idUsuario);
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleCamaraStop(MensajeSocket msg) {
        msg.setIdUsuario(idUsuario);
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleAudio(MensajeSocket msg) { Servidor.broadcast(msg.getRoomCode(), msg, idUsuario); }

    private void handleSalir(MensajeSocket msg) {
        if (roomCode == null) return;
        Sala sala = SalaDAO.buscarPorCodigo(roomCode);
        if (sala != null && sala.getIdHost() == idUsuario) {
            Servidor.cerrarSala(roomCode);
        } else {
            Servidor.broadcast(roomCode, new MensajeSocket.Builder(MensajeSocket.USER_LEFT)
                    .sala(roomCode).usuario(idUsuario, nombreUsuario).build(), idUsuario);
            Servidor.salirDeSala(roomCode, idUsuario);
        }
        roomCode = null;
    }

    private void handleFileStart(MensajeSocket msg) {
        msg.setIdUsuario(idUsuario);
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleFileChunk(MensajeSocket msg) {
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleScreenShare(MensajeSocket msg) {
        msg.setIdUsuario(idUsuario);
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleScreenShareStop(MensajeSocket msg) {
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    private void handleFileEnd(MensajeSocket msg) {
        MensajeSocket notify = new MensajeSocket.Builder(MensajeSocket.FILE_NOTIFY)
                .sala(msg.getRoomCode())
                .archivoInfo(msg.getContenido(), 0)
                .usuario(idUsuario, nombreUsuario)
                .build();
        Servidor.broadcast(msg.getRoomCode(), notify, idUsuario);
    }

    // --- UTILIDADES ---
    public String getNombreUsuario() { return nombreUsuario; }
    public void enviar(MensajeSocket msg) { if (salida != null) salida.println(gson.toJson(msg)); }
    private MensajeSocket error(String txt) { return new MensajeSocket.Builder(MensajeSocket.ERROR).texto(txt).build(); }
    private void desconectar() {
        Servidor.eliminarCliente(idUsuario);
        if (roomCode != null) {
            Servidor.broadcast(roomCode, new MensajeSocket.Builder(MensajeSocket.USER_LEFT)
                    .sala(roomCode).usuario(idUsuario, nombreUsuario).build(), idUsuario);
            Servidor.salirDeSala(roomCode, idUsuario);
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}