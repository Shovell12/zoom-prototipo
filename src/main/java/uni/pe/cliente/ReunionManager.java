package uni.pe.cliente;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ReunionManager {
    private CamaraCaptura camara;
    private MicrofoneCaptura microfono;
    private volatile SourceDataLine lineaAudio;
    private Thread hiloReproductor;
    private final LinkedBlockingQueue<byte[]> colaAudio = new LinkedBlockingQueue<>();

    // --- CÁMARA ---
    public boolean iniciarCamara() { camara = new CamaraCaptura(); return camara.iniciar(); }
    public BufferedImage capturarFrame() { return camara != null ? camara.capturarFrame() : null; }
    public void detenerCamara() { if (camara != null) camara.detener(); }

    // --- MICRÓFONO ---
    public boolean iniciarMicrofono(Mixer.Info info) {
        microfono = new MicrofoneCaptura();
        return info != null ? microfono.iniciar(info) : microfono.iniciar();
    }
    public byte[] capturarAudio() { return microfono != null ? microfono.capturarChunk() : null; }
    public void detenerMicrofono() { if (microfono != null) microfono.detener(); }

    // --- AUDIO (REPRODUCCIÓN) ---
    public void reproducir(byte[] datos) { colaAudio.offer(datos); }
    public void iniciarReproductor(Mixer.Info mixerInfo) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, MicrofoneCaptura.FORMATO);
            lineaAudio = (mixerInfo != null)
                    ? (SourceDataLine) AudioSystem.getMixer(mixerInfo).getLine(info)
                    : (SourceDataLine) AudioSystem.getLine(info);
            lineaAudio.open(MicrofoneCaptura.FORMATO);
            lineaAudio.start();
            hiloReproductor = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        byte[] datos = colaAudio.take();
                        lineaAudio.write(datos, 0, datos.length);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            hiloReproductor.setDaemon(true);
            hiloReproductor.start();
        } catch (LineUnavailableException e) {
            System.err.println("Reproductor de audio no disponible: " + e.getMessage());
        }
    }

    public void detenerReproductor() {
        // Interrumpir primero para salir del take(); cerrar la línea desbloquea cualquier write() en curso.
        if (hiloReproductor != null) hiloReproductor.interrupt();
        if (lineaAudio != null) {
            lineaAudio.stop();
            lineaAudio.close();
            lineaAudio = null;
        }
        if (hiloReproductor != null) {
            try { hiloReproductor.join(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            hiloReproductor = null;
        }
    }

    public void reiniciarReproductor(Mixer.Info mixerInfo) {
        detenerReproductor();
        colaAudio.clear(); // descarta chunks acumulados antes del cambio de dispositivo
        iniciarReproductor(mixerInfo);
    }

    // --- PANTALLA COMPARTIDA ---
    private Robot     robot;
    private Rectangle screenRect;

    public boolean iniciarCompartirPantalla() {
        try {
            robot      = new Robot();
            screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return true;
        } catch (AWTException e) {
            System.err.println("No se puede capturar la pantalla: " + e.getMessage());
            return false;
        }
    }

    public BufferedImage capturarPantalla() {
        return robot != null ? robot.createScreenCapture(screenRect) : null;
    }

    public void detenerCompartirPantalla() { robot = null; }

    // --- ARCHIVOS ---
    public void enviarArchivo(File archivo, String roomCode, ConexionCliente conexion) {
        new Thread(() -> {
            try {
                byte[] datos = Files.readAllBytes(archivo.toPath());
                int CHUNK = 4096;
                int total = (int) Math.ceil((double) datos.length / CHUNK);
                conexion.enviar(new uni.pe.protocolo.MensajeSocket.Builder(uni.pe.protocolo.MensajeSocket.FILE_START)
                        .sala(roomCode).archivoInfo(archivo.getName(), datos.length).build());
                for (int i = 0; i < total; i++) {
                    int desde = i * CHUNK;
                    byte[] chunk = Arrays.copyOfRange(datos, desde, Math.min(desde + CHUNK, datos.length));
                    conexion.enviar(new uni.pe.protocolo.MensajeSocket.Builder(uni.pe.protocolo.MensajeSocket.FILE_CHUNK)
                            .sala(roomCode).archivoChunk(archivo.getName(), Base64.getEncoder().encodeToString(chunk), i, total).build());
                }
                conexion.enviar(new uni.pe.protocolo.MensajeSocket.Builder(uni.pe.protocolo.MensajeSocket.FILE_END).sala(roomCode).texto(archivo.getName()).build());
            } catch (Exception e) {
                    System.err.println("Error al enviar archivo: " + e.getMessage());
                    javax.swing.SwingUtilities.invokeLater(() ->
                            javax.swing.JOptionPane.showMessageDialog(null,
                                    "No se pudo enviar el archivo:\n" + e.getMessage(),
                                    "Error de transferencia", javax.swing.JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
}