package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import java.util.Base64;

/**
 * Traduce mensajes de red (MensajeSocket) a eventos del bus interno.
 * No contiene lógica de negocio: solo mapea tipo de mensaje → tipo de evento.
 */
class ReunionNetworkBridge implements MensajeListener {

    private final ReunionEventBus bus;

    ReunionNetworkBridge(ReunionEventBus bus) {
        this.bus = bus;
    }

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        switch (msg.getType()) {
            case MensajeSocket.USER_JOINED ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.PARTICIPANT_JOINED, msg.getNombreUsuario()));
            case MensajeSocket.USER_LEFT ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.PARTICIPANT_LEFT, msg.getNombreUsuario()));
            case MensajeSocket.CAMERA_FRAME ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.REMOTE_CAMERA_FRAME,
                        msg.getNombreUsuario(), msg.getFrameBase64()));
            case MensajeSocket.CAMERA_STOP ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.REMOTE_CAMERA_STOPPED, msg.getNombreUsuario()));
            case MensajeSocket.AUDIO_FRAME -> {
                if (msg.getAudioBase64() != null)
                    bus.publish(ReunionEvent.of(ReunionEvent.Type.AUDIO_RECEIVED, null,
                            Base64.getDecoder().decode(msg.getAudioBase64())));
            }
            case MensajeSocket.SCREEN_SHARE ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.REMOTE_SCREEN_FRAME, msg.getFrameBase64()));
            case MensajeSocket.SCREEN_SHARE_STOP ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.REMOTE_SCREEN_STOPPED));
            case MensajeSocket.ROOM_CLOSED ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.ROOM_CLOSED, msg.getMensaje()));
        }
    }
}
