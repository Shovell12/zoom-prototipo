package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;

import javax.swing.*;
import java.awt.*;

public class VentanaLogin extends JFrame {

    private final ConexionCliente conexion;
    private JTextField txtCorreo;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnRegistrar;
    private JLabel lblMensaje;

    public VentanaLogin(ConexionCliente conexion) {
        this.conexion = conexion;
        iniciarUI();
    }

    private void iniciarUI() {
        setTitle("Zoom Prototipo");
        setSize(350, 450); // Diseño más vertical y moderno
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel principal con márgenes amplios
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        // --- 1. TÍTULO / LOGO ---
        JLabel titulo = new JLabel("ZOOM", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 42));
        titulo.setForeground(new Color(14, 113, 235)); // Azul oficial
        g.gridx = 0; g.gridy = 0;
        g.insets = new Insets(0, 0, 5, 0);
        panel.add(titulo, g);

        JLabel subtitulo = new JLabel("Prototipo Universitario", SwingConstants.CENTER);
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(Color.GRAY);
        g.gridy = 1;
        g.insets = new Insets(0, 0, 30, 0); // Espacio grande antes del formulario
        panel.add(subtitulo, g);

        // --- 2. CAMPO DE CORREO ---
        // --- 2. CAMPO DE CORREO ---
        txtCorreo = new JTextField();
        txtCorreo.putClientProperty("JTextField.placeholderText", "Correo electrónico");

        // Forma infalible de mostrar la "X" para limpiar el texto
        txtCorreo.putClientProperty("FlatLaf.style", "showClearButton: true");

        txtCorreo.setPreferredSize(new Dimension(250, 40));
        // ... (el resto queda igual)
        txtCorreo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.gridy = 2;
        g.insets = new Insets(0, 0, 15, 0);
        panel.add(txtCorreo, g);

        // --- 3. CAMPO DE CONTRASEÑA ---
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");

        // Forma infalible de mostrar el "Ojo" para revelar la contraseña
        txtPassword.putClientProperty("FlatLaf.style", "showRevealButton: true");

        txtPassword.setPreferredSize(new Dimension(250, 40));
        // ... (el resto queda igual)
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.gridy = 3;
        g.insets = new Insets(0, 0, 25, 0);
        panel.add(txtPassword, g);

        // --- 4. BOTÓN PRINCIPAL (INGRESAR) ---
        btnLogin = new JButton("Ingresar");
        btnLogin.setBackground(new Color(14, 113, 235)); // Azul brillante
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setPreferredSize(new Dimension(250, 40));
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Bordes más redondeados
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        g.gridy = 4;
        g.insets = new Insets(0, 0, 10, 0);
        panel.add(btnLogin, g);

        // --- 5. BOTÓN SECUNDARIO (REGISTRO) ---
        btnRegistrar = new JButton("¿No tienes cuenta? Regístrate");
        btnRegistrar.putClientProperty("JButton.buttonType", "borderless"); // Sin fondo ni borde
        btnRegistrar.setForeground(new Color(14, 113, 235));
        btnRegistrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        g.gridy = 5;
        g.insets = new Insets(0, 0, 15, 0);
        panel.add(btnRegistrar, g);

        // --- 6. MENSAJE DE ERROR ---
        lblMensaje = new JLabel("", SwingConstants.CENTER);
        lblMensaje.setForeground(new Color(224, 40, 40)); // Rojo alerta
        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.gridy = 6;
        g.insets = new Insets(0, 0, 0, 0);
        panel.add(lblMensaje, g);

        add(panel);

        // Acciones
        btnLogin.addActionListener(e -> login());
        btnRegistrar.addActionListener(e -> registrar());
        txtPassword.addActionListener(e -> login()); // Permite ingresar con 'Enter'
    }
    private void login() {
        String correo = txtCorreo.getText().trim();
        String pass   = new String(txtPassword.getPassword());
        if (correo.isEmpty() || pass.isEmpty()) {
            lblMensaje.setText("Completa todos los campos.");
            return;
        }
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.LOGIN_REQUEST);
        msg.setCorreo(correo);
        msg.setPassword(pass);
        conexion.enviar(msg);
    }

    private void registrar() {
        String nombres = JOptionPane.showInputDialog(this, "Ingresa tu nombre completo:");
        if (nombres == null || nombres.isBlank()) return;
        String correo = txtCorreo.getText().trim();
        String pass   = new String(txtPassword.getPassword());
        if (correo.isEmpty() || pass.isEmpty()) {
            lblMensaje.setText("Completa correo y contraseña.");
            return;
        }
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.REGISTER_REQUEST);
        msg.setNombres(nombres);
        msg.setCorreo(correo);
        msg.setPassword(pass);
        conexion.enviar(msg);
    }

    public void mostrarMensaje(String texto, boolean esError) {
        SwingUtilities.invokeLater(() -> {
            lblMensaje.setForeground(esError ? Color.RED : new Color(0, 128, 0));
            lblMensaje.setText(texto);
        });
    }
}