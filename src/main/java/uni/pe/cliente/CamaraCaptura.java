package uni.pe.cliente;

import com.github.sarxos.webcam.Webcam;

import java.awt.image.BufferedImage;

public class CamaraCaptura {

    private Webcam webcam;
    private boolean activa = false;

    public boolean iniciar() {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                System.err.println("No se encontró ninguna cámara.");
                return false;
            }
            webcam.open();
            activa = webcam.isOpen();
            if (!activa) System.err.println("No se pudo abrir la cámara.");
            return activa;
        } catch (Exception e) {
            System.err.println("Error al iniciar cámara: " + e.getMessage());
            return false;
        }
    }

    public BufferedImage capturarFrame() {
        if (!activa || webcam == null || !webcam.isOpen()) return null;
        return webcam.getImage();
    }

    public void detener() {
        activa = false;
        if (webcam != null && webcam.isOpen()) webcam.close();
    }
}
