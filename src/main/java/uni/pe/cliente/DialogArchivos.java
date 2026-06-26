package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import java.awt.*;

/**
 * Ventana emergente de archivos compartidos.
 * Patrón Observer: escucha FILE_NOTIFY para actualizar la lista.
 * Patrón Facade: delega el envío real a ReunionManager.
 */
public class DialogArchivos extends JDialog implements MensajeListener {

    private final ConexionCliente            conexion;
    private final String                     roomCode;
    private final ReunionManager             manager;
    private final DefaultListModel<String>   modeloArchivos = new DefaultListModel<>();

    public DialogArchivos(JFrame owner, ConexionCliente conexion,
                          String roomCode, ReunionManager manager) {
        super(owner, "Archivos compartidos", false);
        this.conexion = conexion;
        this.roomCode = roomCode;
        this.manager  = manager;

        ReunionTheme.estilizarDialog(this, "Archivos compartidos", 300, 400);
        setLayout(new BorderLayout(0, 8));

        JList<String> lista = new JList<>(modeloArchivos);
        lista.setBackground(ReunionTheme.BG_INPUT);
        lista.setForeground(ReunionTheme.TEXT_WHITE);
        lista.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JScrollPane sc = new JScrollPane(lista);
        sc.setBorder(BorderFactory.createLineBorder(ReunionTheme.BORDER));
        sc.getViewport().setBackground(ReunionTheme.BG_INPUT);

        JButton btnEnviar = ReunionTheme.btnPrimario("Enviar archivo");
        btnEnviar.addActionListener(e -> seleccionarYEnviar());

        add(sc,         BorderLayout.CENTER);
        add(btnEnviar,  BorderLayout.SOUTH);

    }

    // ── Observer ─────────────────────────────────────────────────────────────

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        if (MensajeSocket.FILE_NOTIFY.equals(msg.getType())) {
            SwingUtilities.invokeLater(() -> modeloArchivos.addElement(msg.getNombreArchivo()));
        }
    }

    // ── Acción ───────────────────────────────────────────────────────────────

    private void seleccionarYEnviar() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            manager.enviarArchivo(chooser.getSelectedFile(), roomCode, conexion);
    }
}
