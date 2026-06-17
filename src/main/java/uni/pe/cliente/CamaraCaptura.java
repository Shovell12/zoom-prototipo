package uni.pe.cliente;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CamaraCaptura {

    private VideoCapture captura;
    private boolean activa = false;

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("OpenCV no disponible: " + e.getMessage());
        }
    }

    public boolean iniciar() {
        try {
            captura = new VideoCapture(0);
            activa = captura.isOpened();
            if (!activa) System.err.println("No se pudo abrir la cámara.");
            return activa;
        } catch (Exception e) {
            System.err.println("Error al iniciar cámara: " + e.getMessage());
            return false;
        }
    }

    public BufferedImage capturarFrame() {
        if (!activa || captura == null) return null;
        Mat frame = new Mat();
        if (!captura.read(frame) || frame.empty()) return null;
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, buffer);
            byte[] datos = buffer.toArray();
            return ImageIO.read(new ByteArrayInputStream(datos));
        } catch (IOException e) {
            System.err.println("Error al convertir frame: " + e.getMessage());
            return null;
        }
    }

    public void detener() {
        activa = false;
        if (captura != null && captura.isOpened()) captura.release();
    }
}