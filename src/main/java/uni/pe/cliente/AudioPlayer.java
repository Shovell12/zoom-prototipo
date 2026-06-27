package uni.pe.cliente;

import javax.sound.sampled.*;
import java.util.concurrent.LinkedBlockingQueue;

class AudioPlayer {

    private volatile SourceDataLine          lineaAudio;
    private          Thread                  hiloReproductor;
    private final    LinkedBlockingQueue<byte[]> cola = new LinkedBlockingQueue<>();

    void reproducir(byte[] datos) { cola.offer(datos); }

    void iniciar(Mixer.Info mixerInfo) {
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
                        byte[] chunk = cola.take();
                        lineaAudio.write(chunk, 0, chunk.length);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            hiloReproductor.setDaemon(true);
            hiloReproductor.start();
        } catch (LineUnavailableException e) {
            System.err.println("Reproductor de audio no disponible: " + e.getMessage());
        }
    }

    void detener() {
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

    void reiniciar(Mixer.Info mixerInfo) {
        detener();
        cola.clear();
        iniciar(mixerInfo);
    }
}
