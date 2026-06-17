package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;

import javax.swing.*;
import java.awt.*;

public class VentanaPrincipal extends JFrame {

    private final ConexionCliente conexion;
    private final int idUsuario;
    private final String nombreUsuario;
    private JTextField txtCodigoSala, txtNombreSala;
    private JLabel lblBienvenida;

    public VentanaPrincipal(ConexionCliente conexion, int idUsuario, String nombreUsuario) {
        this.conexion = conexion;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        iniciarUI();
    }

    private void iniciarUI() {
        setTitle("Zoom Prototipo — Inicio");
        setSize(420, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 6, 8, 6);

        // Bienvenida
        lblBienvenida = new JLabel("Bienvenido, " + nombreUsuario, SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("Arial", Font.BOLD, 16));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        panel.add(lblBienvenida, g);

        // Separador
        g.gridy = 1;
        panel.add(new JSeparator(), g);

        // Nombre de sala
        g.gridwidth = 1; g.gridy = 2; g.gridx = 0;
        panel.add(new JLabel("Nombre sala:"), g);
        txtNombreSala = new JTextField("Mi sala");
        g.gridx = 1;
        panel.add(txtNombreSala, g);

        // Botón crear sala
        JButton btnCrear = new JButton("Crear sala");
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        panel.add(btnCrear, g);

        // Separador
        g.gridy = 4;
        panel.add(new JSeparator(), g);

        // Código sala
        g.gridwidth = 1; g.gridy = 5; g.gridx = 0;
        panel.add(new JLabel("Código sala:"), g);
        txtCodigoSala = new JTextField();
        g.gridx = 1;
        panel.add(txtCodigoSala, g);

        // Botón unirse
        JButton btnUnirse = new JButton("Unirse a sala");
        g.gridy = 6; g.gridx = 0; g.gridwidth = 2;
        panel.add(btnUnirse, g);

        add(panel);

        // Acciones
        btnCrear.addActionListener(e -> crearSala());
        btnUnirse.addActionListener(e -> unirseASala());
    }

    private void crearSala() {
        String nombre = txtNombreSala.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa un nombre para la sala.");
            return;
        }
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.CREATE_ROOM);
        msg.setRoomName(nombre);
        msg.setIdUsuario(idUsuario);
        conexion.enviar(msg);
    }

    private void unirseASala() {
        String codigo = txtCodigoSala.getText().trim().toUpperCase();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa el código de la sala.");
            return;
        }
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.JOIN_ROOM_REQUEST);
        msg.setRoomCode(codigo);
        msg.setIdUsuario(idUsuario);
        conexion.enviar(msg);
    }
}