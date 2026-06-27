package uni.pe.cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Supplier;

class PanelPantalla extends JPanel {

    private static final int NAME_FONT       = 10;
    private static final int PLACEHOLDER_GAP = 14;

    private final CardLayout cardLayout;
    private final JLabel     lblPantalla;
    private boolean          recibiendoPantalla  = false;
    private boolean          pantallaLocalActiva = false;

    PanelPantalla(ReunionEventBus bus) {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBackground(Color.BLACK);

        add(buildPlaceholder(), "placeholder");

        lblPantalla = new JLabel();
        lblPantalla.setOpaque(true);
        lblPantalla.setBackground(Color.BLACK);
        lblPantalla.setHorizontalAlignment(SwingConstants.CENTER);
        lblPantalla.setVerticalAlignment(SwingConstants.CENTER);
        add(lblPantalla, "screen");

        cardLayout.show(this, "placeholder");

        bus.subscribe(ReunionEvent.Type.REMOTE_SCREEN_FRAME,   e -> mostrarPantalla(e.str()));
        bus.subscribe(ReunionEvent.Type.REMOTE_SCREEN_STOPPED, e -> detenerPantalla());
        bus.subscribe(ReunionEvent.Type.SCREEN_STARTED,        e -> { pantallaLocalActiva = true; cardLayout.show(this, "screen"); });
        bus.subscribe(ReunionEvent.Type.SCREEN_STOPPED,        e -> {
            pantallaLocalActiva = false;
            if (!recibiendoPantalla) {
                lblPantalla.setIcon(null);
                cardLayout.show(this, "placeholder");
            }
        });
    }

    Supplier<Dimension> sizeSupplier() {
        return () -> new Dimension(lblPantalla.getWidth(), lblPantalla.getHeight());
    }

    Consumer<ImageIcon> localScreenCallback() {
        return lblPantalla::setIcon;
    }

    private void mostrarPantalla(String base64) {
        if (base64 == null) return;
        try {
            BufferedImage img = ImageTranscoder.fromBase64(base64);
            if (img == null) return;
            if (!recibiendoPantalla && !pantallaLocalActiva) {
                recibiendoPantalla = true;
                cardLayout.show(this, "screen");
            }
            int w = lblPantalla.getWidth(), h = lblPantalla.getHeight();
            lblPantalla.setIcon(w > 0 && h > 0
                    ? new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_FAST))
                    : new ImageIcon(img));
        } catch (Exception e) {
            System.err.println("Error al decodificar pantalla remota: " + e.getMessage());
        }
    }

    private void detenerPantalla() {
        recibiendoPantalla = false;
        if (!pantallaLocalActiva) {
            lblPantalla.setIcon(null);
            cardLayout.show(this, "placeholder");
        }
    }

    private JPanel buildPlaceholder() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(ReunionTheme.PLACEHOLDER_BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        ImageIcon icono = ReunionTheme.icono("screen_share.png", 64);
        JLabel lblIcono = icono != null ? new JLabel(icono) : new JLabel("[ ]");
        lblIcono.setForeground(ReunionTheme.PLACEHOLDER_ICON);
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTexto = new JLabel("Sin pantalla compartida");
        lblTexto.setForeground(ReunionTheme.PLACEHOLDER_TEXT);
        lblTexto.setFont(new Font("SansSerif", Font.PLAIN, NAME_FONT + 4));
        lblTexto.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(lblIcono);
        inner.add(Box.createVerticalStrut(PLACEHOLDER_GAP));
        inner.add(lblTexto);
        p.add(inner);
        return p;
    }
}
