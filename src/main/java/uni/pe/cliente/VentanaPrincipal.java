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
        setSize(400, 480); // Ventana un poco más alta y espaciosa
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        // --- SALUDO ---
        lblBienvenida = new JLabel("Hola, " + nombreUsuario, SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.gridx = 0; g.gridy = 0;
        g.insets = new Insets(0, 0, 35, 0); // Espacio grande debajo del título
        panel.add(lblBienvenida, g);

        // --- SECCIÓN 1: CREAR SALA ---
        JLabel lblCrear = new JLabel("Nueva Reunión");
        lblCrear.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblCrear.setForeground(new Color(150, 150, 150)); // Gris sutil
        g.gridy = 1;
        g.insets = new Insets(0, 0, 5, 0);
        panel.add(lblCrear, g);

        txtNombreSala = new JTextField("Mi sala");
        txtNombreSala.putClientProperty("JTextField.placeholderText", "Nombre de la sala");
        txtNombreSala.putClientProperty("FlatLaf.style", "showClearButton: true");
        txtNombreSala.setPreferredSize(new Dimension(0, 40)); // Altura moderna
        txtNombreSala.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.gridy = 2;
        g.insets = new Insets(0, 0, 10, 0);
        panel.add(txtNombreSala, g);

        JButton btnCrear = new JButton("Crear Sala");
        btnCrear.setBackground(new Color(14, 113, 235)); // Azul Zoom
        btnCrear.setForeground(Color.WHITE);
        btnCrear.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnCrear.setPreferredSize(new Dimension(0, 40));
        btnCrear.putClientProperty("JButton.buttonType", "roundRect");
        btnCrear.setFocusPainted(false);
        btnCrear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        g.gridy = 3;
        g.insets = new Insets(0, 0, 30, 0); // Espacio grande para separar de la siguiente sección
        panel.add(btnCrear, g);

        // --- SECCIÓN 2: UNIRSE A SALA ---
        JLabel lblUnir = new JLabel("Unirse a una Reunión");
        lblUnir.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblUnir.setForeground(new Color(150, 150, 150));
        g.gridy = 4;
        g.insets = new Insets(0, 0, 5, 0);
        panel.add(lblUnir, g);

        txtCodigoSala = new JTextField();
        txtCodigoSala.putClientProperty("JTextField.placeholderText", "Ingresa el código de la sala");
        txtCodigoSala.putClientProperty("FlatLaf.style", "showClearButton: true");
        txtCodigoSala.setPreferredSize(new Dimension(0, 40));
        txtCodigoSala.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.gridy = 5;
        g.insets = new Insets(0, 0, 10, 0);
        panel.add(txtCodigoSala, g);

        JButton btnUnirse = new JButton("Unirse");
        // Color oscuro y neutro para diferenciarlo de la acción principal de crear
        btnUnirse.setBackground(new Color(60, 60, 60));
        btnUnirse.setForeground(Color.WHITE);
        btnUnirse.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnUnirse.setPreferredSize(new Dimension(0, 40));
        btnUnirse.putClientProperty("JButton.buttonType", "roundRect");
        btnUnirse.setFocusPainted(false);
        btnUnirse.setCursor(new Cursor(Cursor.HAND_CURSOR));
        g.gridy = 6;
        g.insets = new Insets(0, 0, 0, 0);
        panel.add(btnUnirse, g);

        add(panel);

        // Acciones
        btnCrear.addActionListener(e -> crearSala());
        btnUnirse.addActionListener(e -> unirseASala());

        // Atajo: presionar Enter en el código te une automáticamente
        txtCodigoSala.addActionListener(e -> unirseASala());
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