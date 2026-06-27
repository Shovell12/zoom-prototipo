package uni.pe.cliente;

import javax.swing.*;
import java.awt.*;

class PanelToolbar extends JPanel {

    PanelToolbar(boolean esHost, ReunionEventBus bus,
                 DialogChat dialogChat,
                 DialogParticipantes dialogParticipantes,
                 DialogArchivos dialogArchivos) {

        setLayout(new BorderLayout());
        setBackground(ReunionTheme.BG_TOOLBAR);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ReunionTheme.TOOLBAR_SEPARATOR));

        // ── Izquierda: mic y cámara ───────────────────────────────────────────
        JButton btnMic = ReunionTheme.toolbarBtn("Activar Mic",   "mic_off.png");
        JButton btnCam = ReunionTheme.toolbarBtn("Iniciar Video", "cam_off.png");

        btnMic.addActionListener(e -> bus.publish(ReunionEvent.of(ReunionEvent.Type.TOGGLE_MIC)));
        btnCam.addActionListener(e -> bus.publish(ReunionEvent.of(ReunionEvent.Type.TOGGLE_CAMERA)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        left.setBackground(ReunionTheme.BG_TOOLBAR);
        left.add(btnMic);
        left.add(btnCam);

        // ── Centro: herramientas ──────────────────────────────────────────────
        JButton btnPantalla      = ReunionTheme.toolbarBtn("Compartir Pantalla", "screen_share.png");
        JButton btnDispositivos  = ReunionTheme.toolbarBtn("Dispositivos",       "audio.png");
        JButton btnChat          = ReunionTheme.toolbarBtn("Chat",               "chat.png");
        JButton btnParticipantes = ReunionTheme.toolbarBtn("Participantes",      "participants.png");
        JButton btnArchivos      = ReunionTheme.toolbarBtn("Archivos",           "archive.png");

        btnPantalla.addActionListener(e -> bus.publish(ReunionEvent.of(ReunionEvent.Type.TOGGLE_SCREEN)));
        btnDispositivos.addActionListener(e -> bus.publish(ReunionEvent.of(ReunionEvent.Type.OPEN_DEVICES)));
        btnChat.addActionListener(e          -> toggleDialog(dialogChat,          btnChat));
        btnParticipantes.addActionListener(e -> toggleDialog(dialogParticipantes, btnParticipantes));
        btnArchivos.addActionListener(e      -> toggleDialog(dialogArchivos,      btnArchivos));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
        center.setBackground(ReunionTheme.BG_TOOLBAR);
        center.add(btnDispositivos);
        center.add(btnPantalla);
        center.add(btnChat);
        center.add(btnParticipantes);
        center.add(btnArchivos);

        // ── Derecha: salir ────────────────────────────────────────────────────
        JButton btnSalir = ReunionTheme.toolbarBtn(esHost ? "Terminar" : "Salir", "quit.png");
        btnSalir.setBackground(ReunionTheme.DANGER_RED);
        btnSalir.setForeground(ReunionTheme.TEXT_WHITE);
        btnSalir.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnSalir.setBackground(ReunionTheme.DANGER_RED_HOVER); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { btnSalir.setBackground(ReunionTheme.DANGER_RED); }
        });
        btnSalir.addActionListener(e -> bus.publish(ReunionEvent.of(ReunionEvent.Type.LEAVE_MEETING)));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 4));
        right.setBackground(ReunionTheme.BG_TOOLBAR);
        right.add(btnSalir);

        add(left,   BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right,  BorderLayout.EAST);

        // ── Suscripciones: actualizar estado visual de botones ────────────────
        bus.subscribe(ReunionEvent.Type.CAMERA_STARTED, e -> {
            btnCam.setText("Detener Video");
            btnCam.setIcon(ReunionTheme.icono("cam_on.png", 22));
            btnCam.setForeground(ReunionTheme.ZOOM_BLUE);
        });
        bus.subscribe(ReunionEvent.Type.CAMERA_STOPPED, e -> {
            btnCam.setText("Iniciar Video");
            btnCam.setIcon(ReunionTheme.icono("cam_off.png", 22));
            btnCam.setForeground(ReunionTheme.TEXT_GRAY);
        });
        bus.subscribe(ReunionEvent.Type.MIC_STARTED, e -> {
            btnMic.setText("Silenciar");
            btnMic.setIcon(ReunionTheme.icono("mic_on.png", 22));
            btnMic.setForeground(ReunionTheme.ZOOM_BLUE);
        });
        bus.subscribe(ReunionEvent.Type.MIC_STOPPED, e -> {
            btnMic.setText("Activar Mic");
            btnMic.setIcon(ReunionTheme.icono("mic_off.png", 22));
            btnMic.setForeground(ReunionTheme.TEXT_GRAY);
        });
        bus.subscribe(ReunionEvent.Type.SCREEN_STARTED, e -> {
            btnPantalla.setText("Detener Pantalla");
            btnPantalla.setForeground(ReunionTheme.ZOOM_BLUE);
        });
        bus.subscribe(ReunionEvent.Type.SCREEN_STOPPED, e -> {
            btnPantalla.setText("Compartir Pantalla");
            btnPantalla.setIcon(ReunionTheme.icono("screen_share.png", 22));
            btnPantalla.setForeground(ReunionTheme.TEXT_GRAY);
        });
    }

    private void toggleDialog(JDialog dialog, JButton btn) {
        boolean abriendo = !dialog.isVisible();
        if (abriendo && !dialog.isLocationByPlatform()) {
            Window parent = SwingUtilities.getWindowAncestor(this);
            if (parent != null)
                dialog.setLocation(parent.getX() + parent.getWidth() + 8, parent.getY());
        }
        dialog.setVisible(abriendo);
        btn.setForeground(abriendo ? ReunionTheme.ZOOM_BLUE : ReunionTheme.TEXT_GRAY);
    }
}
