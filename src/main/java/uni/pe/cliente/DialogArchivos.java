package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Ventana emergente de archivos compartidos.
 * Observer: escucha FILE_NOTIFY para actualizar la lista.
 * Delega la transferencia y descarga a FileTransferService.
 */
public class DialogArchivos extends JDialog implements MensajeListener {

    private final FileTransferService        fileTransferService;
    private final DefaultListModel<String>   modeloArchivos = new DefaultListModel<>();

    public DialogArchivos(JFrame owner, FileTransferService fileTransferService) {
        super(owner, "Archivos compartidos", false);
        this.fileTransferService = fileTransferService;

        ReunionTheme.estilizarDialog(this, "Archivos compartidos", 300, 400);
        setLayout(new BorderLayout(0, 8));

        JList<String> lista = new JList<>(modeloArchivos);
        lista.setBackground(ReunionTheme.BG_INPUT);
        lista.setForeground(ReunionTheme.TEXT_WHITE);
        lista.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane sc = new JScrollPane(lista);
        sc.setBorder(BorderFactory.createLineBorder(ReunionTheme.BORDER));
        sc.getViewport().setBackground(ReunionTheme.BG_INPUT);

        JButton btnEnviar    = ReunionTheme.btnPrimario("Enviar archivo");
        JButton btnDescargar = ReunionTheme.btnPrimario("Descargar");
        btnDescargar.setEnabled(false);

        btnEnviar.addActionListener(e -> seleccionarYEnviar());
        btnDescargar.addActionListener(e -> descargarSeleccionado(lista));

        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                btnDescargar.setEnabled(lista.getSelectedValue() != null);
        });
        lista.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) descargarSeleccionado(lista);
            }
        });

        JPanel botones = new JPanel(new GridLayout(1, 2, 4, 0));
        botones.setBackground(ReunionTheme.BG_DIALOG);
        botones.add(btnEnviar);
        botones.add(btnDescargar);

        add(sc,      BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    // ── Observer ─────────────────────────────────────────────────────────────

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        if (MensajeSocket.FILE_NOTIFY.equals(msg.getType())) {
            SwingUtilities.invokeLater(() -> modeloArchivos.addElement(msg.getNombreArchivo()));
        }
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    private void seleccionarYEnviar() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            fileTransferService.enviar(chooser.getSelectedFile());
    }

    private void descargarSeleccionado(JList<String> lista) {
        String nombre = lista.getSelectedValue();
        if (nombre != null) fileTransferService.descargar(nombre, this);
    }
}
