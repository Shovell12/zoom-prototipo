package uni.pe.cliente;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ReunionManager {
    private CamaraCaptura camara;
    private MicrofoneCaptura microfono;
    private SourceDataLine lineaAudio;
    private final LinkedBlockingQueue<byte[]> colaAudio = new LinkedBlockingQueue<>();

    // --- CÁMARA ---
    public boolean iniciarCamara() { camara = new CamaraCaptura(); return camara.iniciar(); }
    public BufferedImage capturarFrame() { return camara != null ? camara.capturarFrame() : null; }
    public void detenerCamara() { if (camara != null) camara.detener(); }

    // --- MICRÓFONO ---
    public boolean iniciarMicrofono(Mixer.Info info) {
        microfono = new MicrofoneCaptura();
        return microfono.iniciar(info);
    }
    public byte[] capturarAudio() { return microfono != null ? microfono.capturarChunk() : null; }
    public void detenerMicrofono() { if (microfono != null) microfono.detener(); }

    // --- AUDIO (REPRODUCCIÓN) ---
    public void reproducir(byte[] datos) { colaAudio.offer(datos); }
    public void iniciarReproductor(Mixer.Info mixerInfo) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, MicrofoneCaptura.FORMATO);
        lineaAudio = (mixerInfo != null) ? (SourceDataLine) AudioSystem.getMixer(mixerInfo).getLine(info)
                : (SourceDataLine) AudioSystem.getLine(info);
        lineaAudio.open(MicrofoneCaptura.FORMATO);
        lineaAudio.start();
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] datos = colaAudio.take();
                    lineaAudio.write(datos, 0, datos.length);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }).start();
    }

    // --- ARCHIVOS ---
    public void enviarArchivo(File archivo, String roomCode, ConexionCliente conexion) {
        new Thread(() -> {
            try {
                byte[] datos = Files.readAllBytes(archivo.toPath());
                int CHUNK = 4096;
                int total = (int) Math.ceil((double) datos.length / CHUNK);
                conexion.enviar(new uni.pe.protocolo.MensajeSocket.Builder("FILE_START")
                        .sala(roomCode).archivoInfo(archivo.getName(), datos.length).build());
                for (int i = 0; i < total; i++) {
                    int desde = i * CHUNK;
                    byte[] chunk = Arrays.copyOfRange(datos, desde, Math.min(desde + CHUNK, datos.length));
                    conexion.enviar(new uni.pe.protocolo.MensajeSocket.Builder("FILE_CHUNK")
                            .archivoChunk(archivo.getName(), Base64.getEncoder().encodeToString(chunk), i, total).build());
                }
                conexion.enviar(new uni.pe.protocolo.MensajeSocket.Builder("FILE_END").sala(roomCode).texto(archivo.getName()).build());
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}