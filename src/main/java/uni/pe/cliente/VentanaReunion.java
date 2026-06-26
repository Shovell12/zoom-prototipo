package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Sala de reunión principal.
 *
 * Layout permanente (no cambia al compartir pantalla):
 *   LEFT  (220 px) — pila dinámica de tiles de cámara, scrollable
 *   RIGHT (resto)  — área reservada para pantalla compartida (CardLayout)
 *
 * Patrones:
 *   Observer  — implementa MensajeListener; diálogos hijos también observan.
 *   Command   — ConexionCliente despacha por tipo de mensaje.
 *   Builder   — MensajeSocket.Builder para todos los mensajes salientes.
 *   Facade    — ReunionManager encapsula cámara, micrófono, pantalla y audio.
 *   Singleton — cada diálogo se crea una vez y se muestra/oculta.
 */
public class VentanaReunion extends JFrame implements MensajeListener {

    // ── Estado principal ─────────────────────────────────────────────────────
    private final ConexionCliente  conexion;
    private final ReunionManager   manager = new ReunionManager();
    private final int              idUsuario;
    private final String           nombreUsuario;
    private final String           roomCode;
    private final boolean          esHost;
    private final MediaDispatcher  mediaDispatcher;

    // ── Diálogos emergentes (Singleton por reunión) ───────────────────────────
    private final DialogChat          dialogChat;
    private final DialogParticipantes dialogParticipantes;
    private final DialogArchivos      dialogArchivos;

    // ── Pila de cámaras (lado izquierdo) ─────────────────────────────────────
    private JPanel                    panelCamaras;
    private JLabel                    lblCamaraLocal;
    private final Map<String, JLabel> camarasRemotas     = new LinkedHashMap<>();
    private final Map<String, JPanel> participantesTiles = new LinkedHashMap<>();

    // ── Área de pantalla compartida (lado derecho) ────────────────────────────
    private JPanel     panelPantalla;
    private CardLayout cardPantalla;
    private JLabel     lblPantalla;
    private boolean    recibiendoPantalla = false;
    private boolean    terminada          = false;

    // ── Layout ───────────────────────────────────────────────────────────────
    private static final int WINDOW_W          = 1100;
    private static final int WINDOW_H          = 680;
    private static final int WINDOW_MIN_W      = 820;
    private static final int WINDOW_MIN_H      = 540;
    private static final int CAMERA_STACK_W    = 220;
    private static final int TILE_H            = 140;
    private static final int TILE_MIN_H        = 120;
    private static final int AVATAR_FONT_SIZE  = 28;
    private static final int NAME_FONT_SIZE    = 10;
    private static final int CAMERA_SCALE_H    = 110;
    private static final int PLACEHOLDER_STRUT = 14;
    private static final int TOPBAR_FONT_SIZE  = 13;

    // ── Sesión ───────────────────────────────────────────────────────────────
    private JLabel            lblTiempo;
    private int               segundos = 0;
    private javax.swing.Timer timerSesion;

    // ─────────────────────────────────────────────────────────────────────────

    public VentanaReunion(ReunionConfig config) {
        this.conexion      = config.conexion;
        this.idUsuario     = config.idUsuario;
        this.nombreUsuario = config.nombreUsuario;
        this.roomCode      = config.roomCode;
        this.esHost        = config.esHost;

        dialogChat          = new DialogChat(this, conexion, roomCode, nombreUsuario);
        dialogParticipantes = new DialogParticipantes(this, conexion, roomCode, esHost, nombreUsuario, config.participantesIniciales);
        dialogArchivos      = new DialogArchivos(this, conexion, roomCode, manager);

        conexion.agregarListener(this);
        conexion.agregarListener(dialogChat);
        conexion.agregarListener(dialogParticipantes);
        conexion.agregarListener(dialogArchivos);
        iniciarUI();

        mediaDispatcher = new MediaDispatcher(
                manager, conexion, roomCode,
                icon -> lblCamaraLocal.setIcon(icon),
                ()   -> new Dimension(lblPantalla.getWidth(), lblPantalla.getHeight()),
                icon -> lblPantalla.setIcon(icon)
        );

        for (String p : config.participantesIniciales) agregarParticipanteTile(p);
        manager.iniciarReproductor(PreferenciasAudio.getSalida());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA UI
    // ═════════════════════════════════════════════════════════════════════════

    private void iniciarUI() {
        setTitle("Sala · " + roomCode);
        setSize(WINDOW_W, WINDOW_H);
        setMinimumSize(new Dimension(WINDOW_MIN_W, WINDOW_MIN_H));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { salirReunion(); }
        });

        timerSesion = new javax.swing.Timer(1000, e -> tickTimer());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ReunionTheme.BG_MAIN);
        root.add(buildTopBar(),    BorderLayout.NORTH);
        root.add(buildVideoArea(), BorderLayout.CENTER);
        root.add(buildToolbar(),   BorderLayout.SOUTH);
        setContentPane(root);
        setVisible(true);
        timerSesion.start();
    }

    // ── Barra superior ───────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ReunionTheme.BG_TOOLBAR);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel lblSala = new JLabel("SALA: " + roomCode + (esHost ? "  ·  HOST" : ""));
        lblSala.setForeground(ReunionTheme.TEXT_WHITE);
        lblSala.setFont(new Font("SansSerif", Font.BOLD, TOPBAR_FONT_SIZE));

        lblTiempo = new JLabel("00:00");
        lblTiempo.setForeground(ReunionTheme.TEXT_GRAY);
        lblTiempo.setFont(new Font("SansSerif", Font.PLAIN, TOPBAR_FONT_SIZE));

        bar.add(lblSala,   BorderLayout.WEST);
        bar.add(lblTiempo, BorderLayout.EAST);
        return bar;
    }

    private void tickTimer() {
        segundos++;
        int h = segundos / 3600, m = (segundos % 3600) / 60, s = segundos % 60;
        lblTiempo.setText(h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s));
    }

    // ── Área de video: pila de cámaras (WEST) + pantalla compartida (CENTER) ─

    private JPanel buildVideoArea() {
        JPanel area = new JPanel(new BorderLayout(0, 0));
        area.setBackground(ReunionTheme.BG_MAIN);
        area.add(buildCameraStack(), BorderLayout.WEST);
        area.add(buildScreenArea(),  BorderLayout.CENTER);
        return area;
    }

    // ── Pila dinámica de cámaras (izquierda, 220 px) ─────────────────────────

    private JPanel buildCameraStack() {
        panelCamaras = new JPanel();
        panelCamaras.setLayout(new BoxLayout(panelCamaras, BoxLayout.Y_AXIS));
        panelCamaras.setBackground(ReunionTheme.BG_MAIN);

        lblCamaraLocal = new JLabel();
        lblCamaraLocal.setOpaque(true);
        lblCamaraLocal.setBackground(ReunionTheme.BG_TILE);
        lblCamaraLocal.setHorizontalAlignment(SwingConstants.CENTER);
        panelCamaras.add(buildTile("Tú (" + nombreUsuario + ")", lblCamaraLocal));

        JScrollPane scroll = new JScrollPane(panelCamaras,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ReunionTheme.CAMERA_STACK_BORDER));
        scroll.getViewport().setBackground(ReunionTheme.BG_MAIN);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ReunionTheme.BG_MAIN);
        wrapper.setPreferredSize(new Dimension(CAMERA_STACK_W, 0));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Tile de video ─────────────────────────────────────────────────────────

    private JPanel buildTile(String nombre, JLabel videoLabel) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(ReunionTheme.BG_TILE);
        tile.setPreferredSize(new Dimension(CAMERA_STACK_W, TILE_H));
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, TILE_H));
        tile.setMinimumSize(new Dimension(0, TILE_MIN_H));
        tile.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ReunionTheme.TILE_BORDER));

        JPanel avatarPanel = new JPanel(new GridBagLayout());
        avatarPanel.setBackground(ReunionTheme.BG_TILE);
        JLabel lblIni = new JLabel(iniciales(nombre));
        lblIni.setFont(new Font("SansSerif", Font.BOLD, AVATAR_FONT_SIZE));
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
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, NAME_FONT_SIZE));
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

    // ── Área de pantalla compartida (derecha) ─────────────────────────────────

    private JPanel buildScreenArea() {
        cardPantalla  = new CardLayout();
        panelPantalla = new JPanel(cardPantalla);
        panelPantalla.setBackground(Color.BLACK);

        panelPantalla.add(buildPlaceholder(), "placeholder");

        lblPantalla = new JLabel();
        lblPantalla.setOpaque(true);
        lblPantalla.setBackground(Color.BLACK);
        lblPantalla.setHorizontalAlignment(SwingConstants.CENTER);
        lblPantalla.setVerticalAlignment(SwingConstants.CENTER);
        panelPantalla.add(lblPantalla, "screen");

        cardPantalla.show(panelPantalla, "placeholder");
        return panelPantalla;
    }

    private JPanel buildPlaceholder() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(ReunionTheme.PLACEHOLDER_BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        ImageIcon iconoGrande = ReunionTheme.icono("screen_share.png", 64);
        JLabel lblIcono = iconoGrande != null ? new JLabel(iconoGrande) : new JLabel("[ ]");
        lblIcono.setForeground(ReunionTheme.PLACEHOLDER_ICON);
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTexto = new JLabel("Sin pantalla compartida");
        lblTexto.setForeground(ReunionTheme.PLACEHOLDER_TEXT);
        lblTexto.setFont(new Font("SansSerif", Font.PLAIN, NAME_FONT_SIZE + 4));
        lblTexto.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(lblIcono);
        inner.add(Box.createVerticalStrut(PLACEHOLDER_STRUT));
        inner.add(lblTexto);
        p.add(inner);
        return p;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ReunionTheme.BG_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ReunionTheme.TOOLBAR_SEPARATOR));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        left.setBackground(ReunionTheme.BG_TOOLBAR);

        JButton btnMic = ReunionTheme.toolbarBtn("Activar Mic",   "mic_off.png");
        JButton btnCam = ReunionTheme.toolbarBtn("Iniciar Video", "cam_off.png");

        btnMic.addActionListener(e -> toggleMicrofono(btnMic));
        btnCam.addActionListener(e -> toggleCamara(btnCam));

        left.add(btnMic);
        left.add(btnCam);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
        center.setBackground(ReunionTheme.BG_TOOLBAR);

        JButton btnDispositivos  = ReunionTheme.toolbarBtn("Dispositivos",       "audio.png");
        JButton btnPantalla      = ReunionTheme.toolbarBtn("Compartir Pantalla", "screen_share.png");
        JButton btnChat          = ReunionTheme.toolbarBtn("Chat",               "chat.png");
        JButton btnParticipantes = ReunionTheme.toolbarBtn("Participantes",      "participants.png");
        JButton btnArchivos      = ReunionTheme.toolbarBtn("Archivos",           "archive.png");

        btnDispositivos.addActionListener(e  -> abrirDialogoDispositivos(btnMic));
        btnPantalla.addActionListener(e      -> toggleCompartirPantalla(btnPantalla));
        btnChat.addActionListener(e          -> toggleDialog(dialogChat,          btnChat));
        btnParticipantes.addActionListener(e -> toggleDialog(dialogParticipantes, btnParticipantes));
        btnArchivos.addActionListener(e      -> toggleDialog(dialogArchivos,      btnArchivos));

        center.add(btnDispositivos);
        center.add(btnPantalla);
        center.add(btnChat);
        center.add(btnParticipantes);
        center.add(btnArchivos);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 4));
        right.setBackground(ReunionTheme.BG_TOOLBAR);

        JButton btnSalir = ReunionTheme.toolbarBtn(esHost ? "Terminar" : "Salir", "quit.png");
        btnSalir.setBackground(ReunionTheme.DANGER_RED);
        btnSalir.setForeground(ReunionTheme.TEXT_WHITE);
        btnSalir.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnSalir.setBackground(ReunionTheme.DANGER_RED_HOVER); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { btnSalir.setBackground(ReunionTheme.DANGER_RED); }
        });
        btnSalir.addActionListener(e -> salirReunion());
        right.add(btnSalir);

        bar.add(left,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    private void toggleDialog(JDialog dialog, JButton btn) {
        boolean abriendo = !dialog.isVisible();
        if (abriendo && !dialog.isLocationByPlatform()) {
            dialog.setLocation(getX() + getWidth() + 8, getY());
        }
        dialog.setVisible(abriendo);
        btn.setForeground(abriendo ? ReunionTheme.ZOOM_BLUE : ReunionTheme.TEXT_GRAY);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  OBSERVER
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case MensajeSocket.USER_JOINED ->
                    agregarParticipanteTile(msg.getNombreUsuario());
                case MensajeSocket.USER_LEFT ->
                    eliminarParticipanteTile(msg.getNombreUsuario());
                case MensajeSocket.CAMERA_FRAME ->
                    mostrarFrameRemoto(msg.getNombreUsuario(), msg.getFrameBase64());
                case MensajeSocket.CAMERA_STOP ->
                    limpiarFrameRemoto(msg.getNombreUsuario());
                case MensajeSocket.AUDIO_FRAME -> {
                    if (msg.getAudioBase64() != null)
                        manager.reproducir(Base64.getDecoder().decode(msg.getAudioBase64()));
                }
                case MensajeSocket.SCREEN_SHARE ->
                    mostrarPantallaRemota(msg.getFrameBase64());
                case MensajeSocket.SCREEN_SHARE_STOP ->
                    detenerPantallaRemota();
                case MensajeSocket.ROOM_CLOSED ->
                    cerrarPorExpulsion(msg.getMensaje());
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ACCIONES DE MEDIA (delegan a MediaDispatcher)
    // ═════════════════════════════════════════════════════════════════════════

    private void toggleCamara(JButton btn) {
        if (!mediaDispatcher.isCameraRunning()) {
            if (mediaDispatcher.startCamera()) {
                btn.setText("Detener Video");
                btn.setIcon(ReunionTheme.icono("cam_on.png", 22));
                btn.setForeground(ReunionTheme.ZOOM_BLUE);
            }
        } else {
            mediaDispatcher.stopCamera();
            lblCamaraLocal.setIcon(null);
            btn.setText("Iniciar Video");
            btn.setIcon(ReunionTheme.icono("cam_off.png", 22));
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
    }

    private void toggleMicrofono(JButton btn) {
        if (!mediaDispatcher.isMicActive()) {
            if (mediaDispatcher.startMic(PreferenciasAudio.getMicrofono())) {
                btn.setText("Silenciar");
                btn.setIcon(ReunionTheme.icono("mic_on.png", 22));
                btn.setForeground(ReunionTheme.ZOOM_BLUE);
            }
        } else {
            mediaDispatcher.stopMic();
            btn.setText("Activar Mic");
            btn.setIcon(ReunionTheme.icono("mic_off.png", 22));
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
    }

    private void toggleCompartirPantalla(JButton btn) {
        if (!mediaDispatcher.isScreenRunning()) {
            if (mediaDispatcher.startScreen()) {
                cardPantalla.show(panelPantalla, "screen");
                btn.setText("Detener Pantalla");
                btn.setForeground(ReunionTheme.ZOOM_BLUE);
            }
        } else {
            mediaDispatcher.stopScreen();
            lblPantalla.setIcon(null);
            if (!recibiendoPantalla) cardPantalla.show(panelPantalla, "placeholder");
            btn.setText("Compartir Pantalla");
            btn.setIcon(ReunionTheme.icono("screen_share.png", 22));
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
    }

    private void abrirDialogoDispositivos(JButton btnMic) {
        DialogDispositivos dlg = new DialogDispositivos(this);
        dlg.setVisible(true);
        if (!dlg.isAplicado()) return;

        manager.reiniciarReproductor(PreferenciasAudio.getSalida());

        if (mediaDispatcher.isMicActive()) {
            boolean ok = mediaDispatcher.restartMic(PreferenciasAudio.getMicrofono());
            if (!ok) {
                btnMic.setText("Activar Mic");
                btnMic.setIcon(ReunionTheme.icono("mic_off.png", 22));
                btnMic.setForeground(ReunionTheme.TEXT_GRAY);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ACTUALIZACIÓN DE UI REMOTA
    // ═════════════════════════════════════════════════════════════════════════

    private void agregarParticipanteTile(String usuario) {
        if (usuario == null || usuario.equals(nombreUsuario) || participantesTiles.containsKey(usuario)) return;
        JLabel videoLbl = new JLabel();
        videoLbl.setOpaque(false);
        videoLbl.setHorizontalAlignment(SwingConstants.CENTER);
        camarasRemotas.put(usuario, videoLbl);
        JPanel tile = buildTile(usuario, videoLbl);
        participantesTiles.put(usuario, tile);
        panelCamaras.add(tile);
        panelCamaras.revalidate();
        panelCamaras.repaint();
    }

    private void eliminarParticipanteTile(String usuario) {
        JPanel tile = participantesTiles.remove(usuario);
        if (tile != null) {
            panelCamaras.remove(tile);
            panelCamaras.revalidate();
            panelCamaras.repaint();
        }
        camarasRemotas.remove(usuario);
    }

    private void mostrarFrameRemoto(String usuario, String base64) {
        if (base64 == null || usuario == null) return;
        try {
            BufferedImage img = ImageTranscoder.fromBase64(base64);
            if (img == null) return;
            if (!participantesTiles.containsKey(usuario)) agregarParticipanteTile(usuario);
            JLabel lbl = camarasRemotas.get(usuario);
            if (lbl != null) lbl.setIcon(new ImageIcon(img.getScaledInstance(-1, CAMERA_SCALE_H, Image.SCALE_FAST)));
        } catch (Exception e) {
            System.err.println("Error al decodificar frame de " + usuario + ": " + e.getMessage());
        }
    }

    private void limpiarFrameRemoto(String usuario) {
        JLabel lbl = camarasRemotas.get(usuario);
        if (lbl != null) lbl.setIcon(null);
    }

    private void mostrarPantallaRemota(String base64) {
        if (base64 == null) return;
        try {
            BufferedImage img = ImageTranscoder.fromBase64(base64);
            if (img == null) return;
            if (!recibiendoPantalla && !mediaDispatcher.isScreenRunning()) {
                recibiendoPantalla = true;
                cardPantalla.show(panelPantalla, "screen");
            }
            int w = lblPantalla.getWidth(), h = lblPantalla.getHeight();
            lblPantalla.setIcon(w > 0 && h > 0
                    ? new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_FAST))
                    : new ImageIcon(img));
        } catch (Exception e) {
            System.err.println("Error al decodificar pantalla remota: " + e.getMessage());
        }
    }

    private void detenerPantallaRemota() {
        recibiendoPantalla = false;
        if (!mediaDispatcher.isScreenRunning()) {
            lblPantalla.setIcon(null);
            cardPantalla.show(panelPantalla, "placeholder");
        }
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
        timerSesion.stop();
        mediaDispatcher.stopAll();
        conexion.removerListener(this);
        conexion.removerListener(dialogChat);
        conexion.removerListener(dialogParticipantes);
        conexion.removerListener(dialogArchivos);
        dialogChat.dispose();
        dialogParticipantes.dispose();
        dialogArchivos.dispose();
    }
}
