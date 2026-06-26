package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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
    private final ConexionCliente conexion;
    private final ReunionManager  manager = new ReunionManager();
    private final int     idUsuario;
    private final String  nombreUsuario;
    private final String  roomCode;
    private final boolean esHost;

    // ── Diálogos emergentes (Singleton por reunión) ───────────────────────────
    private final DialogChat          dialogChat;
    private final DialogParticipantes dialogParticipantes;
    private final DialogArchivos      dialogArchivos;

    // ── Pila de cámaras (lado izquierdo) ─────────────────────────────────────
    private JPanel                    panelCamaras;
    private JLabel                    lblCamaraLocal;
    private final Map<String, JLabel> camarasRemotas = new LinkedHashMap<>();

    // ── Área de pantalla compartida (lado derecho) ────────────────────────────
    private JPanel     panelPantalla;
    private CardLayout cardPantalla;
    private JLabel     lblPantalla;
    private boolean    compartiendo       = false;
    private boolean    recibiendoPantalla = false;
    private boolean    terminada          = false;

    // ── Referencia al botón de mic para actualizarlo desde el diálogo ─────────
    private JButton btnMicRef;

    // ── Timers y threads ─────────────────────────────────────────────────────
    private javax.swing.Timer timerCamara;
    private javax.swing.Timer timerPantalla;
    private Thread            hiloMicrofono;

    // ── Sesión ───────────────────────────────────────────────────────────────
    private JLabel            lblTiempo;
    private int               segundos = 0;
    private javax.swing.Timer timerSesion;

    // ─────────────────────────────────────────────────────────────────────────

    public VentanaReunion(ConexionCliente conexion, int idUsuario, String nombre,
                          String roomCode, boolean esHost) {
        this.conexion      = conexion;
        this.idUsuario     = idUsuario;
        this.nombreUsuario = nombre;
        this.roomCode      = roomCode;
        this.esHost        = esHost;

        dialogChat          = new DialogChat(this, conexion, roomCode, nombreUsuario);
        dialogParticipantes = new DialogParticipantes(this, conexion, roomCode, esHost, nombreUsuario);
        dialogArchivos      = new DialogArchivos(this, conexion, roomCode, manager);

        // Registro centralizado: el ciclo de vida de todos los listeners lo gestiona VentanaReunion.
        conexion.agregarListener(this);
        conexion.agregarListener(dialogChat);
        conexion.agregarListener(dialogParticipantes);
        conexion.agregarListener(dialogArchivos);
        iniciarUI();
        manager.iniciarReproductor(PreferenciasAudio.getSalida());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA UI
    // ═════════════════════════════════════════════════════════════════════════

    private void iniciarUI() {
        setTitle("Sala · " + roomCode);
        setSize(1100, 680);
        setMinimumSize(new Dimension(820, 540));
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
        lblSala.setFont(new Font("SansSerif", Font.BOLD, 13));

        lblTiempo = new JLabel("00:00");
        lblTiempo.setForeground(ReunionTheme.TEXT_GRAY);
        lblTiempo.setFont(new Font("SansSerif", Font.PLAIN, 13));

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
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x2E2E2E)));
        scroll.getViewport().setBackground(ReunionTheme.BG_MAIN);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ReunionTheme.BG_MAIN);
        wrapper.setPreferredSize(new Dimension(220, 0));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Tile de video ─────────────────────────────────────────────────────────

    private JPanel buildTile(String nombre, JLabel videoLabel) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(ReunionTheme.BG_TILE);
        tile.setPreferredSize(new Dimension(220, 140));
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        tile.setMinimumSize(new Dimension(0, 120));
        tile.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A2A2A)));

        JPanel avatarPanel = new JPanel(new GridBagLayout());
        avatarPanel.setBackground(ReunionTheme.BG_TILE);
        JLabel lblIni = new JLabel(iniciales(nombre));
        lblIni.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblIni.setForeground(new Color(0x7A7A7A));
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
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblNombre.setForeground(ReunionTheme.TEXT_WHITE);
        lblNombre.setBackground(new Color(0x1A1A1A));
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
        p.setBackground(new Color(0x0E0E0E));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        ImageIcon iconoGrande = ReunionTheme.icono("screen_share.png", 64);
        JLabel lblIcono = iconoGrande != null ? new JLabel(iconoGrande) : new JLabel("[ ]");
        lblIcono.setForeground(new Color(0x444444));
        lblIcono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTexto = new JLabel("Sin pantalla compartida");
        lblTexto.setForeground(new Color(0x555555));
        lblTexto.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblTexto.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(lblIcono);
        inner.add(Box.createVerticalStrut(14));
        inner.add(lblTexto);
        p.add(inner);
        return p;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ReunionTheme.BG_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3A3A3A)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 4));
        left.setBackground(ReunionTheme.BG_TOOLBAR);

        JButton btnMic = ReunionTheme.toolbarBtn("Activar Mic",   "mic_off.png");
        JButton btnCam = ReunionTheme.toolbarBtn("Iniciar Video", "cam_off.png");

        btnMicRef = btnMic;

        btnMic.addActionListener(e -> toggleMicrofono(btnMic));
        btnCam.addActionListener(e -> toggleCamara(btnCam));

        left.add(btnMic);
        left.add(btnCam);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 4));
        center.setBackground(ReunionTheme.BG_TOOLBAR);

        JButton btnDispositivos  = ReunionTheme.toolbarBtn("Dispositivos",      "audio.png");
        JButton btnPantalla      = ReunionTheme.toolbarBtn("Compartir Pantalla","screen_share.png");
        JButton btnChat          = ReunionTheme.toolbarBtn("Chat",              "chat.png");
        JButton btnParticipantes = ReunionTheme.toolbarBtn("Participantes",     "participants.png");
        JButton btnArchivos      = ReunionTheme.toolbarBtn("Archivos",          "archive.png");

        btnDispositivos.addActionListener(e  -> abrirDialogoDispositivos());
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
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnSalir.setBackground(new Color(0xC62828)); }
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
                case MensajeSocket.CAMERA_FRAME ->
                    mostrarFrameRemoto(msg.getNombreUsuario(), msg.getFrameBase64());
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
    //  ACCIONES
    // ═════════════════════════════════════════════════════════════════════════

    // ── Cámara ───────────────────────────────────────────────────────────────

    private void toggleCamara(JButton btn) {
        if (timerCamara == null || !timerCamara.isRunning()) {
            if (manager.iniciarCamara()) {
                timerCamara = new javax.swing.Timer(200, e -> enviarFrame());
                timerCamara.start();
                btn.setText("Detener Video");
                btn.setIcon(ReunionTheme.icono("cam_on.png", 22));
                btn.setForeground(ReunionTheme.ZOOM_BLUE);
            }
        } else {
            timerCamara.stop();
            manager.detenerCamara();
            lblCamaraLocal.setIcon(null);
            btn.setText("Iniciar Video");
            btn.setIcon(ReunionTheme.icono("cam_off.png", 22));
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
    }

    private void enviarFrame() {
        BufferedImage frame = manager.capturarFrame();
        if (frame != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(frame, "jpg", baos);
                lblCamaraLocal.setIcon(
                        new ImageIcon(frame.getScaledInstance(-1, 110, Image.SCALE_FAST)));
                conexion.enviar(new MensajeSocket.Builder(MensajeSocket.CAMERA_FRAME)
                        .sala(roomCode)
                        .frame(Base64.getEncoder().encodeToString(baos.toByteArray()))
                        .build());
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void mostrarFrameRemoto(String usuario, String base64) {
        if (base64 == null || usuario == null) return;
        try {
            BufferedImage img = ImageIO.read(
                    new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            if (img == null) return;
            if (!camarasRemotas.containsKey(usuario)) {
                JLabel videoLbl = new JLabel();
                videoLbl.setOpaque(false);
                videoLbl.setHorizontalAlignment(SwingConstants.CENTER);
                camarasRemotas.put(usuario, videoLbl);
                panelCamaras.add(buildTile(usuario, videoLbl));
                panelCamaras.revalidate();
                panelCamaras.repaint();
            }
            camarasRemotas.get(usuario).setIcon(
                    new ImageIcon(img.getScaledInstance(-1, 110, Image.SCALE_FAST)));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Micrófono ─────────────────────────────────────────────────────────────

    private void toggleMicrofono(JButton btn) {
        if (hiloMicrofono == null || !hiloMicrofono.isAlive()) {
            if (manager.iniciarMicrofono(PreferenciasAudio.getMicrofono())) {
                hiloMicrofono = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) enviarAudio();
                });
                hiloMicrofono.setDaemon(true);
                hiloMicrofono.start();
                btn.setText("Silenciar");
                btn.setIcon(ReunionTheme.icono("mic_on.png", 22));
                btn.setForeground(ReunionTheme.ZOOM_BLUE);
            }
        } else {
            hiloMicrofono.interrupt();
            manager.detenerMicrofono();
            btn.setText("Activar Mic");
            btn.setIcon(ReunionTheme.icono("mic_off.png", 22));
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
    }

    private void enviarAudio() {
        byte[] chunk = manager.capturarAudio();
        if (chunk != null) {
            conexion.enviar(new MensajeSocket.Builder(MensajeSocket.AUDIO_FRAME)
                    .sala(roomCode)
                    .audio(Base64.getEncoder().encodeToString(chunk))
                    .build());
        }
    }

    // ── Pantalla compartida ────────────────────────────────────────────────────

    private void toggleCompartirPantalla(JButton btn) {
        if (!compartiendo) {
            if (manager.iniciarCompartirPantalla()) {
                compartiendo = true;
                cardPantalla.show(panelPantalla, "screen");
                timerPantalla = new javax.swing.Timer(250, e -> enviarPantalla());
                timerPantalla.start();
                btn.setText("Detener Pantalla");
                btn.setForeground(ReunionTheme.ZOOM_BLUE);
            }
        } else {
            timerPantalla.stop();
            manager.detenerCompartirPantalla();
            compartiendo = false;
            lblPantalla.setIcon(null);
            if (!recibiendoPantalla) cardPantalla.show(panelPantalla, "placeholder");
            conexion.enviar(new MensajeSocket.Builder(MensajeSocket.SCREEN_SHARE_STOP)
                    .sala(roomCode).build());
            btn.setText("Compartir Pantalla");
            btn.setIcon(ReunionTheme.icono("screen_share.png", 22));
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
    }

    private void enviarPantalla() {
        BufferedImage pantalla = manager.capturarPantalla();
        if (pantalla != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(pantalla, "jpg", baos);
                int w = lblPantalla.getWidth(), h = lblPantalla.getHeight();
                if (w > 0 && h > 0)
                    lblPantalla.setIcon(
                            new ImageIcon(pantalla.getScaledInstance(w, h, Image.SCALE_FAST)));
                conexion.enviar(new MensajeSocket.Builder(MensajeSocket.SCREEN_SHARE)
                        .sala(roomCode)
                        .frame(Base64.getEncoder().encodeToString(baos.toByteArray()))
                        .build());
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void mostrarPantallaRemota(String base64) {
        if (base64 == null) return;
        try {
            BufferedImage img = ImageIO.read(
                    new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            if (img == null) return;
            if (!recibiendoPantalla && !compartiendo) {
                recibiendoPantalla = true;
                cardPantalla.show(panelPantalla, "screen");
            }
            int w = lblPantalla.getWidth(), h = lblPantalla.getHeight();
            lblPantalla.setIcon(w > 0 && h > 0
                    ? new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_FAST))
                    : new ImageIcon(img));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void detenerPantallaRemota() {
        recibiendoPantalla = false;
        if (!compartiendo) {
            lblPantalla.setIcon(null);
            cardPantalla.show(panelPantalla, "placeholder");
        }
    }

    // ── Dispositivos de audio ─────────────────────────────────────────────────

    private void abrirDialogoDispositivos() {
        DialogDispositivos dlg = new DialogDispositivos(this);
        dlg.setVisible(true);

        if (!dlg.isAplicado()) return;

        // Reiniciar salida de audio con el nuevo dispositivo
        manager.reiniciarReproductor(PreferenciasAudio.getSalida());

        // Si el micrófono está activo, reiniciarlo con el nuevo dispositivo
        boolean micActivo = hiloMicrofono != null && hiloMicrofono.isAlive();
        if (micActivo) {
            hiloMicrofono.interrupt();
            manager.detenerMicrofono();
            if (manager.iniciarMicrofono(PreferenciasAudio.getMicrofono())) {
                hiloMicrofono = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) enviarAudio();
                });
                hiloMicrofono.setDaemon(true);
                hiloMicrofono.start();
            } else {
                // Si falla, actualizar botón a estado apagado
                if (btnMicRef != null) {
                    btnMicRef.setText("Activar Mic");
                    btnMicRef.setIcon(ReunionTheme.icono("mic_off.png", 22));
                    btnMicRef.setForeground(ReunionTheme.TEXT_GRAY);
                }
            }
        }
    }

    // ── Salir ─────────────────────────────────────────────────────────────────

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
        if (timerCamara   != null && timerCamara.isRunning())  timerCamara.stop();
        if (timerPantalla != null && timerPantalla.isRunning()) timerPantalla.stop();
        if (hiloMicrofono != null && hiloMicrofono.isAlive())  hiloMicrofono.interrupt();
        manager.detenerCamara();
        manager.detenerMicrofono();
        manager.detenerReproductor();
        manager.detenerCompartirPantalla();
        conexion.removerListener(this);
        conexion.removerListener(dialogChat);
        conexion.removerListener(dialogParticipantes);
        conexion.removerListener(dialogArchivos);
        dialogChat.dispose();
        dialogParticipantes.dispose();
        dialogArchivos.dispose();
    }
}
