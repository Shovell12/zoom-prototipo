package uni.pe.cliente;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class MicrofoneCaptura {

    public static final AudioFormat FORMATO = new AudioFormat(16000, 16, 1, true, false);
    private static final int BYTES_POR_CHUNK = 1600; // ~50 ms a 16kHz/16bit/mono

    private volatile TargetDataLine linea;
    private volatile boolean activa = false;

    public static List<Mixer.Info> listarMicrofonos() {
        List<Mixer.Info> resultado = new ArrayList<>();
        DataLine.Info lineaInfo = new DataLine.Info(TargetDataLine.class, FORMATO);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mi);
            if (mixer.isLineSupported(lineaInfo)) {
                resultado.add(mi);
            }
        }
        return resultado;
    }

    public static List<Mixer.Info> listarSalidaAudio() {
        List<Mixer.Info> resultado = new ArrayList<>();
        DataLine.Info lineaInfo = new DataLine.Info(SourceDataLine.class, FORMATO);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mi);
            if (mixer.isLineSupported(lineaInfo)) {
                resultado.add(mi);
            }
        }
        return resultado;
    }

    public boolean iniciar(Mixer.Info mixerInfo) {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMATO);
            linea = (TargetDataLine) AudioSystem.getMixer(mixerInfo).getLine(info);
            linea.open(FORMATO);
            linea.start();
            activa = true;
            return true;
        } catch (LineUnavailableException | IllegalArgumentException e) {
            System.err.println("Error al iniciar micrófono: " + e.getMessage());
            if (linea != null) { linea.close(); linea = null; }
            return false;
        }
    }

    public boolean iniciar() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMATO);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Formato de audio no soportado.");
                return false;
            }
            linea = (TargetDataLine) AudioSystem.getLine(info);
            linea.open(FORMATO);
            linea.start();
            activa = true;
            return true;
        } catch (LineUnavailableException e) {
            System.err.println("Error al iniciar micrófono: " + e.getMessage());
            if (linea != null) { linea.close(); linea = null; }
            return false;
        }
    }

    public byte[] capturarChunk() {
        TargetDataLine l = linea; // copia local evita que otro hilo la anule entre el check y el read
        if (!activa || l == null) return null;
        try {
            byte[] buffer = new byte[BYTES_POR_CHUNK];
            int leidos = l.read(buffer, 0, buffer.length);
            if (leidos <= 0) return null;
            if (leidos < buffer.length) {
                byte[] recortado = new byte[leidos];
                System.arraycopy(buffer, 0, recortado, 0, leidos);
                return recortado;
            }
            return buffer;
        } catch (IllegalStateException e) {
            // La línea fue cerrada justo mientras se leía; el hilo captor terminará en el próximo ciclo.
            return null;
        }
    }

    public void detener() {
        activa = false;
        if (linea != null) {
            linea.stop();
            linea.close();
        }
    }
}
