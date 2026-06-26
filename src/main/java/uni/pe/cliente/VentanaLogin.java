package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;

import javax.swing.*;
import java.awt.*;

public class VentanaLogin extends JFrame implements MensajeListener {

    private final ConexionCliente conexion;
    private JTextField txtCorreo;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnRegistrar;
    private JLabel lblMensaje;

    public VentanaLogin(ConexionCliente conexion) {
        this.conexion = conexion;
        this.conexion.agregarListener(this);
        iniciarUI();
    }

    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        switch (msg.getType()) {
            case MensajeSocket.LOGIN_RESPONSE -> {
                if (msg.isExito()) {
                    SwingUtilities.invokeLater(() -> {
                        conexion.removerListener(this);
                        new VentanaPrincipal(conexion, msg.getIdUsuario(), msg.getNombreUsuario()).setVisible(true);
                        dispose();
                    });
                } else {
                    mostrarMensaje(msg.getMensaje(), true);
                }
            }
            case MensajeSocket.REGISTER_RESPONSE ->
                mostrarMensaje(msg.isExito() ? "Registro exitoso. Ahora inicia sesión." : msg.getMensaje(), !msg.isExito());
        }
    }

    private void iniciarUI() {
        setTitle("Zoom Prototipo");
        setSize(350, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        // --- 1. TÍTULO / LOGO ---
        JLabel titulo = new JLabel("ZOOM", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 42));
        titulo.setForeground(new Color(14, 113, 235));
        g.gridx = 0; g.gridy = 0;
        g.insets = new Insets(0, 0, 5, 0);
        panel.add(titulo, g);

        JLabel subtitulo = new JLabel("Prototipo Universitario", SwingConstants.CENTER);
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(Color.GRAY);
        g.gridy = 1;
        g.insets = new Insets(0, 0, 30, 0);
        panel.add(subtitulo, g);

        // --- 2. CAMPO DE CORREO ---
        txtCorreo = new JTextField();
        txtCorreo.putClientProperty("JTextField.placeholderText", "Correo electrónico");
        txtCorreo.putClientProperty("FlatLaf.style", "showClearButton: true");
        txtCorreo.setPreferredSize(new Dimension(250, 40));
        txtCorreo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.gridy = 2;
        g.insets = new Insets(0, 0, 15, 0);
        panel.add(txtCorreo, g);

        // --- 3. CAMPO DE CONTRASEÑA ---
        txtPassword = new JPasswordField();
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtPassword.putClientProperty("FlatLaf.style", "showRevealButton: true");
        txtPassword.setPreferredSize(new Dimension(250, 40));
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.gridy = 3;
        g.insets = new Insets(0, 0, 25, 0);
        panel.add(txtPassword, g);

        // --- 4. BOTÓN PRINCIPAL (INGRESAR) ---
        btnLogin = new JButton("Ingresar");
        btnLogin.setBackground(new Color(14, 113, 235));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setPreferredSize(new Dimension(250, 40));
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        g.gridy = 4;
        g.insets = new Insets(0, 0, 10, 0);
        panel.add(btnLogin, g);

        // --- 5. BOTÓN SECUNDARIO (REGISTRO) ---
        btnRegistrar = new JButton("¿No tienes cuenta? Regístrate");
        btnRegistrar.putClientProperty("JButton.buttonType", "borderless");
        btnRegistrar.setForeground(new Color(14, 113, 235));
        btnRegistrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        g.gridy = 5;
        g.insets = new Insets(0, 0, 15, 0);
        panel.add(btnRegistrar, g);

        // --- 6. MENSAJE DE ERROR ---
        lblMensaje = new JLabel("", SwingConstants.CENTER);
        lblMensaje.setForeground(new Color(224, 40, 40));
        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.gridy = 6;
        g.insets = new Insets(0, 0, 0, 0);
        panel.add(lblMensaje, g);

        add(panel);

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
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.LOGIN_REQUEST)
                .credenciales(correo, pass, null)
                .build());
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
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.REGISTER_REQUEST)
                .credenciales(correo, pass, nombres)
                .build());
    }

    public void mostrarMensaje(String texto, boolean esError) {
        SwingUtilities.invokeLater(() -> {
            lblMensaje.setForeground(esError ? Color.RED : new Color(0, 128, 0));
            lblMensaje.setText(texto);
        });
    }
}