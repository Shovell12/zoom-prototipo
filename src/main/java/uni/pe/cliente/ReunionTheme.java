package uni.pe.cliente;

import javax.swing.*;
import java.awt.*;

/** Paleta y utilidades de UI compartidas por VentanaReunion y sus diálogos. */
final class ReunionTheme {

    static final Color BG_MAIN    = new Color(0x1F1F1F);
    static final Color BG_TOOLBAR = new Color(0x111111);
    static final Color BG_TILE    = new Color(0x3C4043);
    static final Color BG_DIALOG  = new Color(0x242428);
    static final Color BG_INPUT   = new Color(0x2D2D30);
    static final Color TEXT_WHITE = Color.WHITE;
    static final Color TEXT_GRAY  = new Color(0xB0B0B0);
    static final Color ZOOM_BLUE  = new Color(0x0E71EB);
    static final Color DANGER_RED = new Color(0xE53935);

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
        Color hover = new Color(0x3A3A3A);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(DANGER_RED)) btn.setBackground(hover);
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
