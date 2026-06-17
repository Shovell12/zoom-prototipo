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
        setTitle("Zoom Prototipo — Login");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 6, 6, 6);

        // Título
        JLabel titulo = new JLabel("Iniciar Sesión", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        panel.add(titulo, g);

        // Correo
        g.gridwidth = 1; g.gridy = 1; g.gridx = 0;
        panel.add(new JLabel("Correo:"), g);
        txtCorreo = new JTextField();
        g.gridx = 1;
        panel.add(txtCorreo, g);

        // Contraseña
        g.gridy = 2; g.gridx = 0;
        panel.add(new JLabel("Contraseña:"), g);
        txtPassword = new JPasswordField();
        g.gridx = 1;
        panel.add(txtPassword, g);

        // Botones
        btnLogin = new JButton("Ingresar");
        btnRegistrar = new JButton("Registrarse");
        g.gridy = 3; g.gridx = 0;
        panel.add(btnLogin, g);
        g.gridx = 1;
        panel.add(btnRegistrar, g);

        // Mensaje
        lblMensaje = new JLabel("", SwingConstants.CENTER);
        lblMensaje.setForeground(Color.RED);
        g.gridy = 4; g.gridx = 0; g.gridwidth = 2;
        panel.add(lblMensaje, g);

        add(panel);

        // Acciones
        btnLogin.addActionListener(e -> login());
        btnRegistrar.addActionListener(e -> registrar());
        txtPassword.addActionListener(e -> login());
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