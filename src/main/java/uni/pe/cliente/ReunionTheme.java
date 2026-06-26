package uni.pe.cliente;

import javax.swing.*;
import java.awt.*;

/** Paleta y utilidades de UI compartidas por VentanaReunion y sus diálogos. */
final class ReunionTheme {

    // Fondos
    static final Color BG_MAIN    = new Color(0x1F1F1F);
    static final Color BG_TOOLBAR = new Color(0x111111);
    static final Color BG_TILE    = new Color(0x3C4043);
    static final Color BG_DIALOG  = new Color(0x242428);
    static final Color BG_INPUT   = new Color(0x2D2D30);

    // Texto
    static final Color TEXT_WHITE = Color.WHITE;
    static final Color TEXT_GRAY  = new Color(0xB0B0B0);

    // Acento / acción
    static final Color ZOOM_BLUE  = new Color(0x0E71EB);
    static final Color DANGER_RED = new Color(0xE53935);

    // UI de reunión — tiles de cámara
    static final Color TILE_BORDER   = new Color(0x2A2A2A);
    static final Color AVATAR_TEXT   = new Color(0x7A7A7A);
    static final Color NAME_LABEL_BG = new Color(0x1A1A1A);

    // UI de reunión — separadores y hover
    static final Color TOOLBAR_SEPARATOR = new Color(0x3A3A3A);
    static final Color TOOLBAR_BTN_HOVER = new Color(0x3A3A3A);
    static final Color CAMERA_STACK_BORDER = new Color(0x2E2E2E);
    static final Color DANGER_RED_HOVER  = new Color(0xC62828);

    // UI de reunión — placeholder de pantalla compartida
    static final Color PLACEHOLDER_BG   = new Color(0x0E0E0E);
    static final Color PLACEHOLDER_ICON = new Color(0x444444);
    static final Color PLACEHOLDER_TEXT = new Color(0x555555);

    // Controles de diálogo
    static final Color BTN_SECONDARY_BG = new Color(0x4A4A4A);
    static final Color COMBO_BG         = new Color(0x3A3A3A);
    static final Color BORDER           = new Color(0x3A3A3A);
    static final Color BG_INPUT_ALT     = new Color(0x333338);

    private ReunionTheme() {}

    static ImageIcon icono(String nombre, int size) {
        if (nombre == null) return null;
        try {
            java.net.URL url = ReunionTheme.class.getResource("/iconos/" + nombre);
            if (url == null) return null;
            return new ImageIcon(new ImageIcon(url).getImage()
                    .getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception e) { return null; }
    }

    /** Botón de toolbar estilo Zoom: icono arriba, texto debajo. */
    static JButton toolbarBtn(String texto, String nombreIcono) {
        JButton btn = new JButton(texto, icono(nombreIcono, 22));
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setBackground(BG_TOOLBAR);
        btn.setForeground(TEXT_GRAY);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(DANGER_RED)) btn.setBackground(TOOLBAR_BTN_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(DANGER_RED)) btn.setBackground(BG_TOOLBAR);
            }
        });
        return btn;
    }

    /** Botón de acción primaria (azul Zoom). */
    static JButton btnPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(ZOOM_BLUE);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        return btn;
    }

    /** Aplica la estética oscura base a un JDialog. */
    static void estilizarDialog(JDialog d, String titulo, int ancho, int alto) {
        d.setTitle(titulo);
        d.setSize(ancho, alto);
        d.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        d.getContentPane().setBackground(BG_DIALOG);
        ((JPanel) d.getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
    }
}
