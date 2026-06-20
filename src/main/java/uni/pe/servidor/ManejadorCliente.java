package uni.pe.servidor;

import com.google.gson.Gson;
import uni.pe.basedatos.*;
import uni.pe.modelo.Usuario;
import uni.pe.modelo.Sala;
import uni.pe.protocolo.MensajeSocket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class ManejadorCliente implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();
    private PrintWriter salida;
    private int idUsuario;
    private String nombreUsuario;
    private String roomCode;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
        ) {
            this.salida = salida;
            String linea;
            while ((linea = entrada.readLine()) != null) {
                MensajeSocket msg = gson.fromJson(linea, MensajeSocket.class);
                procesarMensaje(msg);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + nombreUsuario);
        } finally {
            desconectar();
        }
    }

    private void procesarMensaje(MensajeSocket msg) {
        switch (msg.getType()) {
            case MensajeSocket.LOGIN_REQUEST      -> handleLogin(msg);
            case MensajeSocket.REGISTER_REQUEST   -> handleRegistro(msg);
            case MensajeSocket.CREATE_ROOM        -> handleCrearSala(msg);
            case MensajeSocket.JOIN_ROOM_REQUEST  -> handleUnirSala(msg);
            case MensajeSocket.ADMIT_USER         -> handleAdmitir(msg);
            case MensajeSocket.CHAT_MESSAGE       -> handleChat(msg);
            case MensajeSocket.FILE_START         -> handleFileStart(msg);
            case MensajeSocket.FILE_CHUNK         -> handleFileChunk(msg);
            case MensajeSocket.FILE_END           -> handleFileEnd(msg);
            case MensajeSocket.CAMERA_FRAME        -> handleCamara(msg);
            case MensajeSocket.FILE_DOWNLOAD_REQUEST -> handleDescarga(msg);
            case MensajeSocket.LEAVE_ROOM         -> handleSalir(msg);
            default -> enviar(error("Tipo de mensaje desconocido: " + msg.getType()));
        }
    }

    // ── LOGIN ──────────────────────────────────────────────────────────────────
    private void handleLogin(MensajeSocket msg) {
        Usuario u = UsuarioDAO.login(msg.getCorreo(), msg.getPassword());
        MensajeSocket resp = new MensajeSocket();
        resp.setType(MensajeSocket.LOGIN_RESPONSE);
        if (u != null) {
            this.idUsuario = u.getIdUsuario();
            this.nombreUsuario = u.getNombres();
            Servidor.registrarCliente(idUsuario, this);
            resp.setExito(true);
            resp.setIdUsuario(u.getIdUsuario());
            resp.setNombreUsuario(u.getNombres());
            resp.setMensaje("Bienvenido, " + u.getNombres());
        } else {
            resp.setExito(false);
            resp.setMensaje("Correo o contraseña incorrectos.");
        }
        enviar(resp);
    }

    // ── REGISTRO ───────────────────────────────────────────────────────────────
    private void handleRegistro(MensajeSocket msg) {
        boolean ok = UsuarioDAO.registrar(msg.getNombres(), msg.getCorreo(), msg.getPassword());
        MensajeSocket resp = new MensajeSocket();
        resp.setType(MensajeSocket.REGISTER_RESPONSE);
        resp.setExito(ok);
        resp.setMensaje(ok ? "Registro exitoso. Ya puedes iniciar sesión." : "El correo ya está registrado.");
        enviar(resp);
    }

    // ── CREAR SALA ─────────────────────────────────────────────────────────────
    private void handleCrearSala(MensajeSocket msg) {
        String codigo = msg.getRoomCode() != null ? msg.getRoomCode()
                : "SALA" + (int)(Math.random() * 9000 + 1000);
        Sala sala = SalaDAO.crear(codigo, msg.getRoomName(), idUsuario);
        MensajeSocket resp = new MensajeSocket();
        resp.setType(MensajeSocket.CREATE_ROOM_RESPONSE);
        if (sala != null) {
            this.roomCode = codigo;
            // El host entra directo como participante aceptado
            SalaDAO.admitirUsuario(sala.getIdSala(), idUsuario);
            Servidor.unirseASala(codigo, idUsuario);
            resp.setExito(true);
            resp.setRoomCode(codigo);
            resp.setMensaje("Sala creada con código: " + codigo);
        } else {
            resp.setExito(false);
            resp.setMensaje("No se pudo crear la sala. El código ya existe.");
        }
        enviar(resp);
    }

    // ── UNIRSE A SALA ──────────────────────────────────────────────────────────
    private void handleUnirSala(MensajeSocket msg) {
        Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
        MensajeSocket resp = new MensajeSocket();
        if (sala == null) {
            resp.setType(MensajeSocket.ERROR);
            resp.setMensaje("Sala no encontrada.");
            enviar(resp);
            return;
        }
        this.roomCode = msg.getRoomCode();
        SalaDAO.registrarSolicitud(sala.getIdSala(), idUsuario);

        // Notificar al host
        MensajeSocket notif = new MensajeSocket();
        notif.setType(MensajeSocket.WAITING_ROOM_UPDATE);
        notif.setIdUsuario(idUsuario);
        notif.setNombreUsuario(nombreUsuario);
        notif.setRoomCode(roomCode);
        Servidor.enviarA(sala.getIdHost(), notif);

        // Confirmar al invitado que está en espera
        resp.setType(MensajeSocket.WAITING_ROOM_UPDATE);
        resp.setMensaje("Esperando aprobación del host...");
        resp.setExito(false);
        enviar(resp);
    }

    // ── ADMITIR / RECHAZAR ─────────────────────────────────────────────────────
    private void handleAdmitir(MensajeSocket msg) {
        Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
        if (sala == null) return;

        MensajeSocket resp = new MensajeSocket();
        resp.setType(MensajeSocket.ADMIT_RESPONSE);
        resp.setRoomCode(msg.getRoomCode());
        resp.setAceptado(msg.isAceptado());

        if (msg.isAceptado()) {
            SalaDAO.admitirUsuario(sala.getIdSala(), msg.getIdUsuario());
            Servidor.unirseASala(msg.getRoomCode(), msg.getIdUsuario());
            resp.setMensaje("Has sido aceptado en la sala.");
        } else {
            SalaDAO.rechazarUsuario(sala.getIdSala(), msg.getIdUsuario());
            resp.setMensaje("El host rechazó tu solicitud.");
        }
        Servidor.enviarA(msg.getIdUsuario(), resp);
    }

    // ── CHAT ───────────────────────────────────────────────────────────────────
    private void handleChat(MensajeSocket msg) {
        Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
        if (sala == null) return;
        MensajeDAO.guardar(sala.getIdSala(), idUsuario, msg.getContenido());
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    // ── ARCHIVOS ───────────────────────────────────────────────────────────────
    private final Map<String, ByteArrayOutputStream> buffers =
            new java.util.concurrent.ConcurrentHashMap<>();

    private void handleFileStart(MensajeSocket msg) {
        buffers.put(msg.getNombreArchivo(), new ByteArrayOutputStream());
        System.out.println("Recibiendo archivo: " + msg.getNombreArchivo());
    }

    private void handleFileChunk(MensajeSocket msg) {
        ByteArrayOutputStream buf = buffers.get(msg.getNombreArchivo());
        if (buf != null) {
            byte[] datos = Base64.getDecoder().decode(msg.getChunkBase64());
            buf.write(datos, 0, datos.length);
        }
    }

    private void handleFileEnd(MensajeSocket msg) {
        ByteArrayOutputStream buf = buffers.remove(msg.getNombreArchivo());
        if (buf == null) return;
        try {
            Path carpeta = Paths.get("archivos", msg.getRoomCode());
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve(msg.getNombreArchivo());
            Files.write(destino, buf.toByteArray());

            Sala sala = SalaDAO.buscarPorCodigo(msg.getRoomCode());
            if (sala != null) {
                ArchivoDAO.registrar(sala.getIdSala(), idUsuario,
                        msg.getNombreArchivo(), destino.toString(), buf.size());
            }

            // Notificar a la sala
            MensajeSocket notif = new MensajeSocket();
            notif.setType(MensajeSocket.FILE_NOTIFY);
            notif.setNombreArchivo(msg.getNombreArchivo());
            notif.setNombreUsuario(nombreUsuario);
            notif.setRoomCode(msg.getRoomCode());
            Servidor.broadcast(msg.getRoomCode(), notif, idUsuario);
            System.out.println("Archivo guardado: " + destino);
        } catch (IOException e) {
            System.err.println("Error al guardar archivo: " + e.getMessage());
        }
    }

    // ── DESCARGA ───────────────────────────────────────────────────────────────
    private void handleDescarga(MensajeSocket msg) {
        Path archivo = Paths.get("archivos", msg.getRoomCode(), msg.getNombreArchivo());
        if (!Files.exists(archivo)) {
            enviar(error("Archivo no encontrado: " + msg.getNombreArchivo()));
            return;
        }
        try {
            byte[] datos = Files.readAllBytes(archivo);
            int CHUNK = 4096;
            int total = (int) Math.ceil((double) datos.length / CHUNK);

            MensajeSocket start = new MensajeSocket();
            start.setType(MensajeSocket.FILE_START);
            start.setNombreArchivo(msg.getNombreArchivo());
            start.setTamanio(datos.length);
            start.setTotalChunks(total);
            enviar(start);

            for (int i = 0; i < total; i++) {
                int desde = i * CHUNK;
                int hasta = Math.min(desde + CHUNK, datos.length);
                byte[] chunk = new byte[hasta - desde];
                System.arraycopy(datos, desde, chunk, 0, chunk.length);
                MensajeSocket chunkMsg = new MensajeSocket();
                chunkMsg.setType(MensajeSocket.FILE_CHUNK);
                chunkMsg.setNombreArchivo(msg.getNombreArchivo());
                chunkMsg.setChunkBase64(Base64.getEncoder().encodeToString(chunk));
                chunkMsg.setChunkIndex(i);
                chunkMsg.setTotalChunks(total);
                enviar(chunkMsg);
            }

            MensajeSocket end = new MensajeSocket();
            end.setType(MensajeSocket.FILE_END);
            end.setNombreArchivo(msg.getNombreArchivo());
            enviar(end);
        } catch (IOException e) {
            enviar(error("Error al leer el archivo: " + e.getMessage()));
        }
    }

    // ── CÁMARA ─────────────────────────────────────────────────────────────────
    private void handleCamara(MensajeSocket msg) {
        msg.setIdUsuario(idUsuario);
        msg.setNombreUsuario(nombreUsuario);
        Servidor.broadcast(msg.getRoomCode(), msg, idUsuario);
    }

    // ── SALIR ──────────────────────────────────────────────────────────────────
    private void handleSalir(MensajeSocket msg) {
        if (roomCode != null) {
            Servidor.salirDeSala(roomCode, idUsuario);
            roomCode = null;
        }
    }

    // ── UTILIDADES ─────────────────────────────────────────────────────────────
    public void enviar(MensajeSocket msg) {
        if (salida != null) salida.println(gson.toJson(msg));
    }

    private MensajeSocket error(String texto) {
        MensajeSocket e = new MensajeSocket();
        e.setType(MensajeSocket.ERROR);
        e.setMensaje(texto);
        return e;
    }

    private void desconectar() {
        Servidor.eliminarCliente(idUsuario);
        if (roomCode != null) Servidor.salirDeSala(roomCode, idUsuario);
        try { socket.close(); } catch (IOException ignored) {}
    }
}