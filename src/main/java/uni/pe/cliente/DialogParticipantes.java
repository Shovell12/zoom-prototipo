package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ventana emergente de participantes.
 * Muestra quién está en la llamada y (para el host) la sala de espera.
 *
 * Patrones:
 *   Observer — escucha CAMERA_FRAME / CHAT_MESSAGE para descubrir participantes
 *              y WAITING_ROOM_UPDATE para la sala de espera.
 *   Builder  — MensajeSocket.Builder para enviar ADMIT_USER.
 */
public class DialogParticipantes extends JDialog implements MensajeListener {

    private final ConexionCliente           conexion;
    private final String                    roomCode;
    private final boolean                   esHost;

    // Participantes en la llamada
    private final DefaultListModel<String>  modeloParticipantes = new DefaultListModel<>();
    private       JLabel                    lblConteo;

    // Sala de espera (solo host)
    private final DefaultListModel<String>  modeloEspera        = new DefaultListModel<>();
    private final Map<String, Integer>      mapaEspera          = new HashMap<>();
    private       JList<String>             listaEspera;
    private       JLabel                    lblEsperaConteo;

    public DialogParticipantes(JFrame owner, ConexionCliente conexion,
                               String roomCode, boolean esHost, String miNombre) {
        super(owner, "Participantes", false);
        this.conexion = conexion;
        this.roomCode = roomCode;
        this.esHost   = esHost;

        modeloParticipantes.addElement(miNombre + " (Tú)");

        ReunionTheme.estilizarDialog(this, "Participantes", 300, esHost ? 520 : 420);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

    }

    // ── Header: código de sala ────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(ReunionTheme.BG_DIALOG);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel lblTitulo = new JLabel("Código de sala");
        lblTitulo.setForeground(ReunionTheme.TEXT_GRAY);
        lblTitulo.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JLabel lblCodigo = new JLabel(roomCode);
        lblCodigo.setForeground(ReunionTheme.TEXT_WHITE);
        lblCodigo.setFont(new Font("SansSerif", Font.BOLD, 20));

        p.add(lblTitulo, BorderLayout.NORTH);
        p.add(lblCodigo, BorderLayout.CENTER);
        return p;
    }

    // ── Cuerpo: participantes + sala de espera ─────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ReunionTheme.BG_DIALOG);

        // — Sección "EN LA REUNIÓN" —
        lblConteo = sectionLabel("EN LA REUNIÓN", 1);
        body.add(lblConteo);
        body.add(Box.createVerticalStrut(6));

        JList<String> listaParticipantes = new JList<>(modeloParticipantes);
        listaParticipantes.setBackground(ReunionTheme.BG_INPUT);
        listaParticipantes.setForeground(ReunionTheme.TEXT_WHITE);
        listaParticipantes.setFont(new Font("SansSerif", Font.PLAIN, 13));
        listaParticipantes.setCellRenderer(new ParticipantRenderer());
        listaParticipantes.setFixedCellHeight(36);

        JScrollPane scrollP = new JScrollPane(listaParticipantes);
        scrollP.setBorder(BorderFactory.createLineBorder(new Color(0x3A3A3A)));
        scrollP.getViewport().setBackground(ReunionTheme.BG_INPUT);
        int altoPart = esHost ? 200 : 320;
        scrollP.setPreferredSize(new Dimension(Integer.MAX_VALUE, altoPart));
        scrollP.setMaximumSize(new Dimension(Integer.MAX_VALUE, altoPart));
        scrollP.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(scrollP);

        // — Sección "SALA DE ESPERA" (solo host) —
        if (esHost) {
            body.add(Box.createVerticalStrut(14));
            lblEsperaConteo = sectionLabel("SALA DE ESPERA", 0);
            body.add(lblEsperaConteo);
            body.add(Box.createVerticalStrut(6));
            body.add(buildPanelEspera());
        }

        return body;
    }

    private JLabel sectionLabel(String texto, int count) {
        JLabel lbl = new JLabel(texto + " (" + count + ")");
        lbl.setForeground(ReunionTheme.TEXT_GRAY);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setBorder(new EmptyBorder(0, 0, 0, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel buildPanelEspera() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(ReunionTheme.BG_DIALOG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        listaEspera = new JList<>(modeloEspera);
        listaEspera.setBackground(ReunionTheme.BG_INPUT);
        listaEspera.setForeground(ReunionTheme.TEXT_WHITE);
        listaEspera.setFont(new Font("SansSerif", Font.PLAIN, 13));
        listaEspera.setFixedCellHeight(34);

        JScrollPane sc = new JScrollPane(listaEspera);
        sc.setBorder(BorderFactory.createLineBorder(new Color(0x3A3A3A)));
        sc.getViewport().setBackground(ReunionTheme.BG_INPUT);
        p.add(sc, BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(1, 2, 6, 0));
        botones.setBackground(ReunionTheme.BG_DIALOG);

        JButton btnAdmitir = ReunionTheme.btnPrimario("Admitir");
        btnAdmitir.addActionListener(e -> gestionar(true));

        JButton btnRechazar = new JButton("Rechazar");
        btnRechazar.setBackground(ReunionTheme.DANGER_RED);
        btnRechazar.setForeground(ReunionTheme.TEXT_WHITE);
        btnRechazar.setFocusPainted(false);
        btnRechazar.setBorder(new EmptyBorder(8, 0, 8, 0));
        btnRechazar.putClientProperty("JButton.buttonType", "roundRect");
        btnRechazar.addActionListener(e -> gestionar(false));

        botones.add(btnAdmitir);
        botones.add(btnRechazar);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case MensajeSocket.CAMERA_FRAME, MensajeSocket.CHAT_MESSAGE, MensajeSocket.AUDIO_FRAME -> {
                    String nombre = msg.getNombreUsuario();
                    if (nombre != null && !modeloParticipantes.contains(nombre)
                            && !modeloParticipantes.contains(nombre + " (Tú)")) {
                        modeloParticipantes.addElement(nombre);
                        lblConteo.setText("EN LA REUNIÓN (" + modeloParticipantes.size() + ")");
                    }
                }
                case MensajeSocket.WAITING_ROOM_UPDATE -> {
                    String nombre = msg.getNombreUsuario();
                    if (nombre != null && !mapaEspera.containsKey(nombre)) {
                        mapaEspera.put(nombre, msg.getIdUsuario());
                        modeloEspera.addElement(nombre);
                        if (lblEsperaConteo != null)
                            lblEsperaConteo.setText("SALA DE ESPERA (" + modeloEspera.size() + ")");
                    }
                }
            }
        });
    }

    // ── Admitir / rechazar ────────────────────────────────────────────────────

    private void gestionar(boolean aceptar) {
        String sel = listaEspera.getSelectedValue();
        if (sel == null) return;
        MensajeSocket msg = new MensajeSocket.Builder(MensajeSocket.ADMIT_USER)
                .sala(roomCode).usuario(mapaEspera.get(sel), sel).build();
        msg.setAceptado(aceptar);
        conexion.enviar(msg);
        modeloEspera.removeElement(sel);
        mapaEspera.remove(sel);
        if (lblEsperaConteo != null)
            lblEsperaConteo.setText("SALA DE ESPERA (" + modeloEspera.size() + ")");
    }

    // ── Renderer personalizado para la lista de participantes ─────────────────

    private static class ParticipantRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            String texto = value.toString();
            boolean esLocal = texto.endsWith("(Tú)");
            lbl.setFont(new Font("SansSerif", esLocal ? Font.BOLD : Font.PLAIN, 13));
            if (!isSelected) {
                lbl.setBackground(index % 2 == 0 ? ReunionTheme.BG_INPUT : new Color(0x333338));
                lbl.setForeground(esLocal ? ReunionTheme.TEXT_WHITE : ReunionTheme.TEXT_GRAY);
            }
            lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
            lbl.setText("  " + texto);
            return lbl;
        }
    }
}
