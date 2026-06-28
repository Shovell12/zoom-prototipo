package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

class FileTransferService implements MensajeListener {

    private static final int CHUNK_SIZE = 4096;

    private final ConexionCliente conexion;
    private final String          roomCode;

    private final ConcurrentHashMap<String, byte[][]> chunksEnProgreso  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, byte[]>   archivosRecibidos = new ConcurrentHashMap<>();

    FileTransferService(ConexionCliente conexion, String roomCode) {
        this.conexion = conexion;
        this.roomCode = roomCode;
    }

    // ── Envío ─────────────────────────────────────────────────────────────────

    void enviar(File archivo) {
        new Thread(() -> {
            try {
                byte[] datos = Files.readAllBytes(archivo.toPath());
                int total = (int) Math.ceil((double) datos.length / CHUNK_SIZE);

                conexion.enviar(new MensajeSocket.Builder(MensajeSocket.FILE_START)
                        .sala(roomCode).archivoInfo(archivo.getName(), datos.length).build());

                for (int i = 0; i < total; i++) {
                    int desde = i * CHUNK_SIZE;
                    byte[] chunk = Arrays.copyOfRange(datos, desde, Math.min(desde + CHUNK_SIZE, datos.length));
                    conexion.enviar(new MensajeSocket.Builder(MensajeSocket.FILE_CHUNK)
                            .sala(roomCode)
                            .archivoChunk(archivo.getName(), Base64.getEncoder().encodeToString(chunk), i, total)
                            .build());
                }

                conexion.enviar(new MensajeSocket.Builder(MensajeSocket.FILE_END)
                        .sala(roomCode).texto(archivo.getName()).build());

            } catch (Exception e) {
                System.err.println("Error al enviar archivo: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null,
                                "No se pudo enviar el archivo:\n" + e.getMessage(),
                                "Error de transferencia", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    // ── Recepción ─────────────────────────────────────────────────────────────

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        if (!MensajeSocket.FILE_CHUNK.equals(msg.getType())) return;

        String nombre = msg.getNombreArchivo();
        int    total  = msg.getTotalChunks();

        chunksEnProgreso.computeIfAbsent(nombre, k -> new byte[total][]);
        chunksEnProgreso.get(nombre)[msg.getChunkIndex()] =
                Base64.getDecoder().decode(msg.getChunkBase64());

        byte[][] chunks = chunksEnProgreso.get(nombre);
        for (byte[] c : chunks) {
            if (c == null) return;
        }
        ensamblar(nombre, chunks);
    }

    private void ensamblar(String nombre, byte[][] chunks) {
        int totalBytes = 0;
        for (byte[] c : chunks) totalBytes += c.length;
        byte[] datos = new byte[totalBytes];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, datos, pos, c.length);
            pos += c.length;
        }
        archivosRecibidos.put(nombre, datos);
        chunksEnProgreso.remove(nombre);
    }

    // ── Descarga ──────────────────────────────────────────────────────────────

    void descargar(String nombre, Component parent) {
        byte[] datos = archivosRecibidos.get(nombre);
        if (datos == null) {
            JOptionPane.showMessageDialog(parent,
                    "El archivo aún no está disponible para descargar.",
                    "No disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(nombre));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File destino = chooser.getSelectedFile();
        new Thread(() -> {
            try {
                Files.write(destino.toPath(), datos);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent,
                                "Archivo guardado en:\n" + destino.getAbsolutePath(),
                                "Descarga completa", JOptionPane.INFORMATION_MESSAGE));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(parent,
                                "Error al guardar el archivo:\n" + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
}
