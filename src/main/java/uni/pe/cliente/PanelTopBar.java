package uni.pe.cliente;

import javax.swing.*;
import java.awt.*;

class PanelTopBar extends JPanel {

    private static final int FONT_SIZE = 13;

    private final JLabel             lblTiempo;
    private final javax.swing.Timer  timer;
    private int                      segundos = 0;

    PanelTopBar(String roomCode, boolean esHost) {
        setLayout(new BorderLayout());
        setBackground(ReunionTheme.BG_TOOLBAR);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel lblSala = new JLabel("SALA: " + roomCode + (esHost ? "  ·  HOST" : ""));
        lblSala.setForeground(ReunionTheme.TEXT_WHITE);
        lblSala.setFont(new Font("SansSerif", Font.BOLD, FONT_SIZE));

        lblTiempo = new JLabel("00:00");
        lblTiempo.setForeground(ReunionTheme.TEXT_GRAY);
        lblTiempo.setFont(new Font("SansSerif", Font.PLAIN, FONT_SIZE));

        add(lblSala,   BorderLayout.WEST);
        add(lblTiempo, BorderLayout.EAST);

        timer = new javax.swing.Timer(1000, e -> tick());
        timer.start();
    }

    void detener() { timer.stop(); }

    private void tick() {
        segundos++;
        int h = segundos / 3600, m = (segundos % 3600) / 60, s = segundos % 60;
        lblTiempo.setText(h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s));
    }
}
