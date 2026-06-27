package uni.pe.cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;

class PanelCamaras extends JPanel {

    private static final int STACK_W         = 220;
    private static final int TILE_H          = 140;
    private static final int TILE_MIN_H      = 120;
    private static final int AVATAR_FONT     = 28;
    private static final int NAME_FONT       = 10;
    private static final int CAMERA_SCALE_H  = 110;

    private final String               nombreUsuario;
    private final JPanel               innerStack;
    private final JLabel               lblCamaraLocal;
    private final Map<String, JLabel>  camarasRemotas     = new LinkedHashMap<>();
    private final Map<String, JPanel>  participantesTiles = new LinkedHashMap<>();

    PanelCamaras(String nombreUsuario, ReunionEventBus bus) {
        this.nombreUsuario = nombreUsuario;

        innerStack = new JPanel();
        innerStack.setLayout(new BoxLayout(innerStack, BoxLayout.Y_AXIS));
        innerStack.setBackground(ReunionTheme.BG_MAIN);

        lblCamaraLocal = new JLabel();
        lblCamaraLocal.setOpaque(true);
        lblCamaraLocal.setBackground(ReunionTheme.BG_TILE);
        lblCamaraLocal.setHorizontalAlignment(SwingConstants.CENTER);
        innerStack.add(buildTile("Tú (" + nombreUsuario + ")", lblCamaraLocal));

        JScrollPane scroll = new JScrollPane(innerStack,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ReunionTheme.CAMERA_STACK_BORDER));
        scroll.getViewport().setBackground(ReunionTheme.BG_MAIN);

        setLayout(new BorderLayout());
        setBackground(ReunionTheme.BG_MAIN);
        setPreferredSize(new Dimension(STACK_W, 0));
        add(scroll, BorderLayout.CENTER);

        bus.subscribe(ReunionEvent.Type.PARTICIPANT_JOINED,   e -> agregarTile(e.str()));
        bus.subscribe(ReunionEvent.Type.PARTICIPANT_LEFT,     e -> eliminarTile(e.str()));
        bus.subscribe(ReunionEvent.Type.REMOTE_CAMERA_FRAME,  e -> mostrarFrame(e.str(), (String) e.extra()));
        bus.subscribe(ReunionEvent.Type.REMOTE_CAMERA_STOPPED, e -> limpiarFrame(e.str()));
        bus.subscribe(ReunionEvent.Type.CAMERA_STOPPED,       e -> lblCamaraLocal.setIcon(null));
    }

    Consumer<ImageIcon> localCameraCallback() {
        return lblCamaraLocal::setIcon;
    }

    private void agregarTile(String usuario) {
        if (usuario == null || usuario.equals(nombreUsuario) || participantesTiles.containsKey(usuario)) return;
        JLabel videoLbl = new JLabel();
        videoLbl.setOpaque(false);
        videoLbl.setHorizontalAlignment(SwingConstants.CENTER);
        camarasRemotas.put(usuario, videoLbl);
        JPanel tile = buildTile(usuario, videoLbl);
        participantesTiles.put(usuario, tile);
        innerStack.add(tile);
        innerStack.revalidate();
        innerStack.repaint();
    }

    private void eliminarTile(String usuario) {
        JPanel tile = participantesTiles.remove(usuario);
        if (tile != null) {
            innerStack.remove(tile);
            innerStack.revalidate();
            innerStack.repaint();
        }
        camarasRemotas.remove(usuario);
    }

    private void mostrarFrame(String usuario, String base64) {
        if (base64 == null || usuario == null) return;
        try {
            BufferedImage img = ImageTranscoder.fromBase64(base64);
            if (img == null) return;
            if (!participantesTiles.containsKey(usuario)) agregarTile(usuario);
            JLabel lbl = camarasRemotas.get(usuario);
            if (lbl != null)
                lbl.setIcon(new ImageIcon(img.getScaledInstance(-1, CAMERA_SCALE_H, Image.SCALE_FAST)));
        } catch (Exception ex) {
            System.err.println("Error al decodificar frame de " + usuario + ": " + ex.getMessage());
        }
    }

    private void limpiarFrame(String usuario) {
        JLabel lbl = camarasRemotas.get(usuario);
        if (lbl != null) lbl.setIcon(null);
    }

    private JPanel buildTile(String nombre, JLabel videoLabel) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(ReunionTheme.BG_TILE);
        tile.setPreferredSize(new Dimension(STACK_W, TILE_H));
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, TILE_H));
        tile.setMinimumSize(new Dimension(0, TILE_MIN_H));
        tile.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ReunionTheme.TILE_BORDER));

        JPanel avatarPanel = new JPanel(new GridBagLayout());
        avatarPanel.setBackground(ReunionTheme.BG_TILE);
        JLabel lblIni = new JLabel(iniciales(nombre));
        lblIni.setFont(new Font("SansSerif", Font.BOLD, AVATAR_FONT));
        lblIni.setForeground(ReunionTheme.AVATAR_TEXT);
        avatarPanel.add(lblIni);

        JLayeredPane layered = new JLayeredPane() {
            @Override public void doLayout() {
                for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        layered.setBackground(ReunionTheme.BG_TILE);
        layered.setOpaque(true);
        layered.add(avatarPanel, JLayeredPane.DEFAULT_LAYER);
        layered.add(videoLabel,  JLayeredPane.PALETTE_LAYER);
        videoLabel.setOpaque(false);
        videoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblNombre = new JLabel("  " + nombre);
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, NAME_FONT));
        lblNombre.setForeground(ReunionTheme.TEXT_WHITE);
        lblNombre.setBackground(ReunionTheme.NAME_LABEL_BG);
        lblNombre.setOpaque(true);
        lblNombre.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

        tile.add(layered,   BorderLayout.CENTER);
        tile.add(lblNombre, BorderLayout.SOUTH);
        return tile;
    }

    private String iniciales(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++)
            if (!partes[i].isEmpty()) sb.append(Character.toUpperCase(partes[i].charAt(0)));
        return sb.length() > 0 ? sb.toString() : "?";
    }
}
