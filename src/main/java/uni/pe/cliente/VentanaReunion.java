package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import java.awt.*;

/**
 * Sala de reunión principal.
 *
 * Responsabilidad única: ensamblar componentes y gestionar el ciclo de vida.
 *
 * Layout:
 *   NORTH  — PanelTopBar   (sala, host, cronómetro)
 *   WEST   — PanelCamaras  (pila de tiles de cámara)
 *   CENTER — PanelPantalla (pantalla compartida / placeholder)
 *   SOUTH  — PanelToolbar  (controles)
 *
 * Comunicación interna: ReunionEventBus desacopla paneles y controladores.
 *   Red  → ReunionNetworkBridge  → bus → paneles
 *   UI   → bus → ReunionMediaController → MediaDispatcher → bus → paneles
 */
public class VentanaReunion extends JFrame {

    private final ConexionCliente conexion;
    private final ReunionManager  manager     = new ReunionManager();
    private final AudioPlayer     audioPlayer = new AudioPlayer();
    private final ReunionEventBus bus         = new ReunionEventBus();
    private final String          roomCode;

    private final FileTransferService  fileTransferService;
    private final DialogChat          dialogChat;
    private final DialogParticipantes dialogParticipantes;
    private final DialogArchivos      dialogArchivos;

    private final PanelTopBar  topBar;
    private final PanelCamaras panelCamaras;

    private final ReunionNetworkBridge   networkBridge;
    private final ReunionMediaController mediaController;
    private final MediaDispatcher        mediaDispatcher;

    private boolean terminada = false;

    public VentanaReunion(ReunionConfig config) {
        this.conexion = config.conexion;
        this.roomCode = config.roomCode;

        fileTransferService = new FileTransferService(conexion, config.roomCode);

        dialogChat          = new DialogChat(this, conexion, config.roomCode, config.nombreUsuario);
        dialogParticipantes = new DialogParticipantes(this, conexion, config.roomCode,
                config.esHost, config.nombreUsuario, config.participantesIniciales);
        dialogArchivos      = new DialogArchivos(this, fileTransferService);

        topBar       = new PanelTopBar(config.roomCode, config.esHost);
        panelCamaras = new PanelCamaras(config.nombreUsuario, bus);
        PanelPantalla panelPantalla = new PanelPantalla(bus);
        PanelToolbar  toolbar       = new PanelToolbar(config.esHost, bus,
                dialogChat, dialogParticipantes, dialogArchivos);

        mediaDispatcher = new MediaDispatcher(
                manager, audioPlayer, conexion, config.roomCode,
                panelCamaras.localCameraCallback(),
                panelPantalla.sizeSupplier(),
                panelPantalla.localScreenCallback()
        );

        networkBridge   = new ReunionNetworkBridge(bus);
        mediaController = new ReunionMediaController(bus, mediaDispatcher, audioPlayer, this);

        bus.subscribe(ReunionEvent.Type.ROOM_CLOSED,   e -> cerrarPorExpulsion(e.str()));
        bus.subscribe(ReunionEvent.Type.LEAVE_MEETING, e -> salirReunion());

        conexion.agregarListener(networkBridge);
        conexion.agregarListener(fileTransferService);
        conexion.agregarListener(dialogChat);
        conexion.agregarListener(dialogParticipantes);
        conexion.agregarListener(dialogArchivos);

        montarUI(panelCamaras, panelPantalla, toolbar);

        config.participantesIniciales.forEach(p ->
                bus.publish(ReunionEvent.of(ReunionEvent.Type.PARTICIPANT_JOINED, p)));
        audioPlayer.iniciar(PreferenciasAudio.getSalida());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MONTAJE DE UI
    // ═════════════════════════════════════════════════════════════════════════

    private void montarUI(PanelCamaras camaras, PanelPantalla pantalla, PanelToolbar toolbar) {
        setTitle("Sala · " + roomCode);
        setSize(1100, 680);
        setMinimumSize(new Dimension(820, 540));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                bus.publish(ReunionEvent.of(ReunionEvent.Type.LEAVE_MEETING));
            }
        });

        JPanel videoArea = new JPanel(new BorderLayout(0, 0));
        videoArea.setBackground(ReunionTheme.BG_MAIN);
        videoArea.add(camaras,  BorderLayout.WEST);
        videoArea.add(pantalla, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ReunionTheme.BG_MAIN);
        root.add(topBar,     BorderLayout.NORTH);
        root.add(videoArea,  BorderLayout.CENTER);
        root.add(toolbar,    BorderLayout.SOUTH);
        setContentPane(root);
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CICLO DE VIDA
    // ═════════════════════════════════════════════════════════════════════════

    private void salirReunion() {
        detenerRecursos();
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.LEAVE_ROOM)
                .sala(roomCode).build());
        conexion.cerrar();
        dispose();
    }

    private void cerrarPorExpulsion(String razon) {
        detenerRecursos();
        conexion.cerrar();
        dispose();
        JOptionPane.showMessageDialog(null,
                razon != null ? razon : "La reunión ha finalizado.",
                "Reunión finalizada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void detenerRecursos() {
        if (terminada) return;
        terminada = true;
        topBar.detener();
        mediaDispatcher.stopAll();
        conexion.removerListener(networkBridge);
        conexion.removerListener(fileTransferService);
        conexion.removerListener(dialogChat);
        conexion.removerListener(dialogParticipantes);
        conexion.removerListener(dialogArchivos);
        dialogChat.dispose();
        dialogParticipantes.dispose();
        dialogArchivos.dispose();
    }
}
