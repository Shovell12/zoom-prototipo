package uni.pe.cliente;

import javax.sound.sampled.*;
import java.util.prefs.Preferences;

public class PreferenciasAudio {

    private static final Preferences PREFS    = Preferences.userNodeForPackage(PreferenciasAudio.class);
    private static final String      KEY_MIC  = "mic_dispositivo";
    private static final String      KEY_OUT  = "salida_dispositivo";

    public static Mixer.Info getMicrofono() {
        return buscar(PREFS.get(KEY_MIC, null), TargetDataLine.class);
    }

    public static void setMicrofono(Mixer.Info info) {
        if (info == null) PREFS.remove(KEY_MIC);
        else              PREFS.put(KEY_MIC, info.getName());
    }

    public static Mixer.Info getSalida() {
        return buscar(PREFS.get(KEY_OUT, null), SourceDataLine.class);
    }

    public static void setSalida(Mixer.Info info) {
        if (info == null) PREFS.remove(KEY_OUT);
        else              PREFS.put(KEY_OUT, info.getName());
    }

    private static Mixer.Info buscar(String nombre, Class<? extends DataLine> tipo) {
        if (nombre == null) return null;
        DataLine.Info lineaInfo = new DataLine.Info(tipo, MicrofoneCaptura.FORMATO);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            if (mi.getName().equals(nombre) && AudioSystem.getMixer(mi).isLineSupported(lineaInfo))
                return mi;
        }
        return null;
    }
}
