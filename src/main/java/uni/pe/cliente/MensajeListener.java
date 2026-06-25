package uni.pe.cliente;
import uni.pe.protocolo.MensajeSocket;

public interface MensajeListener {
    void onMensajeRecibido(MensajeSocket msg);
}
