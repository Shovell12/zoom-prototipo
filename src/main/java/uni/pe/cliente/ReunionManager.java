package uni.pe.cliente;

import javax.sound.sampled.Mixer;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ReunionManager {

    private CamaraCaptura    camara;
    private MicrofoneCaptura microfono;
    private Robot            robot;
    private Rectangle        screenRect;

    // ── Cámara ────────────────────────────────────────────────────────────────

    public boolean iniciarCamara() { camara = new CamaraCaptura(); return camara.iniciar(); }
    public BufferedImage capturarFrame() { return camara != null ? camara.capturarFrame() : null; }
    public void detenerCamara() { if (camara != null) camara.detener(); }

    // ── Micrófono ─────────────────────────────────────────────────────────────

    public boolean iniciarMicrofono(Mixer.Info info) {
        microfono = new MicrofoneCaptura();
        return info != null ? microfono.iniciar(info) : microfono.iniciar();
    }
    public byte[] capturarAudio() { return microfono != null ? microfono.capturarChunk() : null; }
    public void detenerMicrofono() { if (microfono != null) microfono.detener(); }

    // ── Pantalla compartida ───────────────────────────────────────────────────

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
    public BufferedImage capturarPantalla() { return robot != null ? robot.createScreenCapture(screenRect) : null; }
    public void detenerCompartirPantalla()  { robot = null; }
}
