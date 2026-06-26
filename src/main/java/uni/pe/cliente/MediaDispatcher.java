package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.sound.sampled.Mixer;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Gestiona los bucles de captura y envío de media (cámara, micrófono, pantalla).
 * Desacoplado de la UI: recibe callbacks para actualizar vistas locales.
 */
class MediaDispatcher {

    private static final int CAMERA_INTERVAL_MS = 200;
    private static final int SCREEN_INTERVAL_MS = 250;
    private static final int CAMERA_SCALE_H     = 110;

    private final ReunionManager      manager;
    private final ConexionCliente     conexion;
    private final String              roomCode;
    private final Consumer<ImageIcon> onLocalFrame;    // preview cámara local
    private final Supplier<Dimension> pantallaSize;    // dimensiones del label de pantalla
    private final Consumer<ImageIcon> onPantallaFrame; // preview pantalla local

    private javax.swing.Timer timerCamara;
    private javax.swing.Timer timerPantalla;
    private Thread            hiloMicrofono;

    MediaDispatcher(ReunionManager manager, ConexionCliente conexion, String roomCode,
                    Consumer<ImageIcon> onLocalFrame,
                    Supplier<Dimension> pantallaSize,
                    Consumer<ImageIcon> onPantallaFrame) {
        this.manager         = manager;
        this.conexion        = conexion;
        this.roomCode        = roomCode;
        this.onLocalFrame    = onLocalFrame;
        this.pantallaSize    = pantallaSize;
        this.onPantallaFrame = onPantallaFrame;
    }

    // ── Cámara ────────────────────────────────────────────────────────────────

    boolean isCameraRunning() {
        return timerCamara != null && timerCamara.isRunning();
    }

    boolean startCamera() {
        if (!manager.iniciarCamara()) return false;
        timerCamara = new javax.swing.Timer(CAMERA_INTERVAL_MS, e -> enviarFrame());
        timerCamara.start();
        return true;
    }

    void stopCamera() {
        if (timerCamara != null) timerCamara.stop();
        manager.detenerCamara();
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.CAMERA_STOP)
                .sala(roomCode).build());
    }

    private void enviarFrame() {
        BufferedImage frame = manager.capturarFrame();
        if (frame == null) return;
        try {
            onLocalFrame.accept(
                    new ImageIcon(frame.getScaledInstance(-1, CAMERA_SCALE_H, Image.SCALE_FAST)));
            conexion.enviar(new MensajeSocket.Builder(MensajeSocket.CAMERA_FRAME)
                    .sala(roomCode)
                    .frame(ImageTranscoder.toBase64Jpg(frame))
                    .build());
        } catch (IOException e) {
            System.err.println("Error al codificar frame de cámara: " + e.getMessage());
        }
    }

    // ── Micrófono ─────────────────────────────────────────────────────────────

    boolean isMicActive() {
        return hiloMicrofono != null && hiloMicrofono.isAlive();
    }

    boolean startMic(Mixer.Info micInfo) {
        if (!manager.iniciarMicrofono(micInfo)) return false;
        hiloMicrofono = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) enviarAudio();
        });
        hiloMicrofono.setDaemon(true);
        hiloMicrofono.start();
        return true;
    }

    void stopMic() {
        if (hiloMicrofono != null) hiloMicrofono.interrupt();
        manager.detenerMicrofono();
    }

    boolean restartMic(Mixer.Info micInfo) {
        stopMic();
        return startMic(micInfo);
    }

    private void enviarAudio() {
        byte[] chunk = manager.capturarAudio();
        if (chunk != null) {
            conexion.enviar(new MensajeSocket.Builder(MensajeSocket.AUDIO_FRAME)
                    .sala(roomCode)
                    .audio(Base64.getEncoder().encodeToString(chunk))
                    .build());
        }
    }

    // ── Pantalla compartida ───────────────────────────────────────────────────

    boolean isScreenRunning() {
        return timerPantalla != null && timerPantalla.isRunning();
    }

    boolean startScreen() {
        if (!manager.iniciarCompartirPantalla()) return false;
        timerPantalla = new javax.swing.Timer(SCREEN_INTERVAL_MS, e -> enviarPantalla());
        timerPantalla.start();
        return true;
    }

    void stopScreen() {
        if (timerPantalla != null) timerPantalla.stop();
        manager.detenerCompartirPantalla();
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.SCREEN_SHARE_STOP)
                .sala(roomCode).build());
    }

    private void enviarPantalla() {
        BufferedImage pantalla = manager.capturarPantalla();
        if (pantalla == null) return;
        try {
            Dimension d = pantallaSize.get();
            if (d.width > 0 && d.height > 0)
                onPantallaFrame.accept(
                        new ImageIcon(pantalla.getScaledInstance(d.width, d.height, Image.SCALE_FAST)));
            conexion.enviar(new MensajeSocket.Builder(MensajeSocket.SCREEN_SHARE)
                    .sala(roomCode)
                    .frame(ImageTranscoder.toBase64Jpg(pantalla))
                    .build());
        } catch (IOException e) {
            System.err.println("Error al codificar frame de pantalla: " + e.getMessage());
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    void stopAll() {
        if (timerCamara   != null && timerCamara.isRunning())   timerCamara.stop();
        if (timerPantalla != null && timerPantalla.isRunning())  timerPantalla.stop();
        if (hiloMicrofono != null && hiloMicrofono.isAlive())    hiloMicrofono.interrupt();
        manager.detenerCamara();
        manager.detenerMicrofono();
        manager.detenerReproductor();
        manager.detenerCompartirPantalla();
    }
}
