package uni.pe.cliente;

import javax.swing.JFrame;

/**
 * Suscribe eventos del bus que requieren acción sobre los dispositivos de media.
 * Publica de vuelta el resultado (STARTED / STOPPED) para que la UI actualice su estado.
 */
class ReunionMediaController {

    private final ReunionEventBus bus;
    private final MediaDispatcher dispatcher;
    private final AudioPlayer     audioPlayer;
    private final JFrame          owner;

    ReunionMediaController(ReunionEventBus bus, MediaDispatcher dispatcher,
                           AudioPlayer audioPlayer, JFrame owner) {
        this.bus         = bus;
        this.dispatcher  = dispatcher;
        this.audioPlayer = audioPlayer;
        this.owner       = owner;

        bus.subscribe(ReunionEvent.Type.AUDIO_RECEIVED, e -> audioPlayer.reproducir((byte[]) e.extra()));
        bus.subscribe(ReunionEvent.Type.TOGGLE_CAMERA,  e -> handleToggleCamera());
        bus.subscribe(ReunionEvent.Type.TOGGLE_MIC,     e -> handleToggleMic());
        bus.subscribe(ReunionEvent.Type.TOGGLE_SCREEN,  e -> handleToggleScreen());
        bus.subscribe(ReunionEvent.Type.OPEN_DEVICES,   e -> handleOpenDevices());
    }

    private void handleToggleCamera() {
        if (!dispatcher.isCameraRunning()) {
            if (dispatcher.startCamera())
                bus.publish(ReunionEvent.of(ReunionEvent.Type.CAMERA_STARTED));
        } else {
            dispatcher.stopCamera();
            bus.publish(ReunionEvent.of(ReunionEvent.Type.CAMERA_STOPPED));
        }
    }

    private void handleToggleMic() {
        if (!dispatcher.isMicActive()) {
            if (dispatcher.startMic(PreferenciasAudio.getMicrofono()))
                bus.publish(ReunionEvent.of(ReunionEvent.Type.MIC_STARTED));
        } else {
            dispatcher.stopMic();
            bus.publish(ReunionEvent.of(ReunionEvent.Type.MIC_STOPPED));
        }
    }

    private void handleToggleScreen() {
        if (!dispatcher.isScreenRunning()) {
            if (dispatcher.startScreen())
                bus.publish(ReunionEvent.of(ReunionEvent.Type.SCREEN_STARTED));
        } else {
            dispatcher.stopScreen();
            bus.publish(ReunionEvent.of(ReunionEvent.Type.SCREEN_STOPPED));
        }
    }

    private void handleOpenDevices() {
        DialogDispositivos dlg = new DialogDispositivos(owner);
        dlg.setVisible(true);
        if (!dlg.isAplicado()) return;

        audioPlayer.reiniciar(PreferenciasAudio.getSalida());

        if (dispatcher.isMicActive()) {
            boolean ok = dispatcher.restartMic(PreferenciasAudio.getMicrofono());
            if (!ok) bus.publish(ReunionEvent.of(ReunionEvent.Type.MIC_STOPPED));
        }
    }
}
