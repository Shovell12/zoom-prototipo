package uni.pe.servidor;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;

/**
 * Ventana de control del servidor: muestra IP/puerto y permite detenerlo.
 */
class ServidorVentana extends JFrame {

    private final ServerSocket    serverSocket;
    private final ExecutorService pool;

    ServidorVentana(ServerSocket serverSocket, ExecutorService pool, String ip, int puerto) {
        super("Servidor activo");
        this.serverSocket = serverSocket;
        this.pool         = pool;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(300, 160);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel etiqueta = new JLabel(
                "<html><center>Servidor en ejecución.<br/>IP: <b>" + ip + "</b><br/>Puerto: <b>" + puerto + "</b></center></html>",
                SwingConstants.CENTER);
        add(etiqueta, BorderLayout.CENTER);

        JButton btnDetener = new JButton("Detener servidor");
        btnDetener.addActionListener(e -> detener());
        JPanel panel = new JPanel();
        panel.add(btnDetener);
        add(panel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { detener(); }
        });
    }

    private void detener() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Detener el servidor? Se desconectarán todos los clientes.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        Servidor.notificarCierrePorServidor();
        try { serverSocket.close(); } catch (IOException ex) {
            System.err.println("Error al cerrar servidor: " + ex.getMessage());
        }
        pool.shutdownNow();
        System.out.println("Servidor detenido.");
        dispose();
        System.exit(0);
    }
}
