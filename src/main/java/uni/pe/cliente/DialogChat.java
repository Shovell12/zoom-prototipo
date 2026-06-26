package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import java.awt.*;

/**
 * Ventana emergente de chat de reunión.
 * Patrón Observer: se registra como MensajeListener para recibir CHAT_MESSAGE.
 * Patrón Builder: usa MensajeSocket.Builder para enviar mensajes.
 */
public class DialogChat extends JDialog implements MensajeListener {

    private final ConexionCliente conexion;
    private final String          roomCode;
    private final String          miNombre;
    private final JTextArea       areaChat;
    private final JTextField      txtMensaje;

    public DialogChat(JFrame owner, ConexionCliente conexion, String roomCode, String miNombre) {
        super(owner, "Chat", false);
        this.conexion  = conexion;
        this.roomCode  = roomCode;
        this.miNombre  = miNombre;

        ReunionTheme.estilizarDialog(this, "Chat de la reunión", 340, 500);
        setLayout(new BorderLayout(0, 8));

        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setBackground(ReunionTheme.BG_INPUT);
        areaChat.setForeground(ReunionTheme.TEXT_WHITE);
        areaChat.setFont(new Font("SansSerif", Font.PLAIN, 13));
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true);
        areaChat.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(areaChat);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x3A3A3A)));
        scroll.getViewport().setBackground(ReunionTheme.BG_INPUT);

        txtMensaje = new JTextField();
        txtMensaje.setBackground(ReunionTheme.BG_INPUT);
        txtMensaje.setForeground(ReunionTheme.TEXT_WHITE);
        txtMensaje.setCaretColor(ReunionTheme.TEXT_WHITE);
        txtMensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtMensaje.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3A3A3A)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        txtMensaje.putClientProperty("JTextField.placeholderText", "Escribe un mensaje…");
        txtMensaje.addActionListener(e -> enviar());

        JButton btnEnviar = ReunionTheme.btnPrimario("→");
        btnEnviar.addActionListener(e -> enviar());

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(ReunionTheme.BG_DIALOG);
        inputRow.add(txtMensaje, BorderLayout.CENTER);
        inputRow.add(btnEnviar,  BorderLayout.EAST);

        add(scroll,   BorderLayout.CENTER);
        add(inputRow, BorderLayout.SOUTH);

    }

    // ── Observer ─────────────────────────────────────────────────────────────

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        if (MensajeSocket.CHAT_MESSAGE.equals(msg.getType())) {
            SwingUtilities.invokeLater(() ->
                    areaChat.append(msg.getNombreUsuario() + ": " + msg.getContenido() + "\n"));
        }
    }

    // ── Acción ───────────────────────────────────────────────────────────────

    private void enviar() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty()) return;
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.CHAT_MESSAGE)
                .sala(roomCode).texto(texto).build());
        areaChat.append("Yo: " + texto + "\n");
        txtMensaje.setText("");
    }
}
