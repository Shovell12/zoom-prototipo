package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;

class FileTransferService {

    private static final int CHUNK_SIZE = 4096;

    private final ConexionCliente conexion;
    private final String          roomCode;

    FileTransferService(ConexionCliente conexion, String roomCode) {
        this.conexion = conexion;
        this.roomCode = roomCode;
    }

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
}
