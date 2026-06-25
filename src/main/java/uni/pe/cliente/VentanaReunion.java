package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

public class VentanaReunion extends JFrame implements MensajeListener {
    private final ConexionCliente conexion;
    private final ReunionManager manager = new ReunionManager();
    private final int idUsuario;
    private final String roomCode;
    private final boolean esHost;

    // Componentes UI
    private JTextArea areaChat;
    private JTextField txtMensaje;
    private DefaultListModel<String> modeloEspera, modeloArchivos;
    private JList<String> listaEspera, listaArchivos;
    private JPanel panelCamaras;
    private JLabel lblCamaraLocal;

    // Estados
    private final Map<String, Integer> mapaEspera = new HashMap<>();
    private final Map<String, JLabel> camarasRemotas = new HashMap<>();
    private javax.swing.Timer timerCamara;

    public VentanaReunion(ConexionCliente conexion, int idUsuario, String nombre, String roomCode, boolean esHost) {
        this.conexion = conexion;
        this.idUsuario = idUsuario;
        this.roomCode = roomCode;
        this.esHost = esHost;
        this.conexion.agregarListener(this); // Registro al Observer
        iniciarUI();
    }

    private void iniciarUI() {
        setTitle("Sala: " + roomCode + (esHost ? " [HOST]" : ""));
        setSize(900, 650);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new BorderLayout(5, 5));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. PANEL IZQUIERDO: CÁMARAS + ESPERA
        JPanel panelIzq = new JPanel(new BorderLayout(10, 10));
        panelIzq.setPreferredSize(new Dimension(300, 0));
        panelCamaras = new JPanel(new GridLayout(0, 1, 0, 10));
        lblCamaraLocal = camaraLabel("Tu cámara");
        panelCamaras.add(lblCamaraLocal);
        panelIzq.add(new JScrollPane(panelCamaras), BorderLayout.CENTER);

        if (esHost) {
            modeloEspera = new DefaultListModel<>();
            listaEspera = new JList<>(modeloEspera);
            JPanel panelEspera = new JPanel(new BorderLayout());
            panelEspera.setBorder(BorderFactory.createTitledBorder("Sala de espera"));
            panelEspera.add(new JScrollPane(listaEspera), BorderLayout.CENTER);
            JButton btnAceptar = new JButton("Aceptar");
            btnAceptar.addActionListener(e -> gestionarEspera(true));
            panelEspera.add(btnAceptar, BorderLayout.SOUTH);
            panelIzq.add(panelEspera, BorderLayout.SOUTH);
        }

        // 2. PANEL CENTRAL: CHAT
        JPanel panelChat = new JPanel(new BorderLayout(5, 5));
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        txtMensaje = new JTextField();
        JButton btnEnviar = new JButton("Enviar");
        btnEnviar.addActionListener(e -> enviarMensaje());
        JPanel pEnvio = new JPanel(new BorderLayout());
        pEnvio.add(txtMensaje, BorderLayout.CENTER);
        pEnvio.add(btnEnviar, BorderLayout.EAST);
        panelChat.add(new JScrollPane(areaChat), BorderLayout.CENTER);
        panelChat.add(pEnvio, BorderLayout.SOUTH);

        // 3. PANEL DERECHO: ARCHIVOS
        JPanel panelDer = new JPanel(new BorderLayout());
        modeloArchivos = new DefaultListModel<>();
        listaArchivos = new JList<>(modeloArchivos);
        JButton btnEnviarArchivo = new JButton("Enviar archivo");
        btnEnviarArchivo.addActionListener(e -> enviarArchivo());
        panelDer.add(new JScrollPane(listaArchivos), BorderLayout.CENTER);
        panelDer.add(btnEnviarArchivo, BorderLayout.SOUTH);

        // 4. CONTROLES INFERIORES
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnCamara = new JButton("Iniciar Video");
        btnCamara.addActionListener(e -> toggleCamara(btnCamara));
        panelControles.add(btnCamara);

        panelPrincipal.add(panelIzq, BorderLayout.WEST);
        panelPrincipal.add(panelChat, BorderLayout.CENTER);
        panelPrincipal.add(panelDer, BorderLayout.EAST);
        panelPrincipal.add(panelControles, BorderLayout.SOUTH);

        add(panelPrincipal);
        setVisible(true);
    }

    // --- LÓGICA DE EVENTOS (Observer) ---
    @Override
    public void onMensajeRecibido(MensajeSocket msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case MensajeSocket.CHAT_MESSAGE -> areaChat.append(msg.getNombreUsuario() + ": " + msg.getContenido() + "\n");
                case MensajeSocket.CAMERA_FRAME -> mostrarFrameRemoto(msg.getNombreUsuario(), msg.getFrameBase64());
                case MensajeSocket.WAITING_ROOM_UPDATE -> agregarAEspera(msg.getIdUsuario(), msg.getNombreUsuario());
                case MensajeSocket.FILE_NOTIFY -> modeloArchivos.addElement(msg.getNombreArchivo());
            }
        });
    }

    // --- ACCIONES ---
    private void enviarMensaje() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty()) return;
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.CHAT_MESSAGE).sala(roomCode).texto(texto).build());
        areaChat.append("Yo: " + texto + "\n");
        txtMensaje.setText("");
    }

    private void toggleCamara(JButton btn) {
        if (timerCamara == null || !timerCamara.isRunning()) {
            if (manager.iniciarCamara()) {
                timerCamara = new javax.swing.Timer(200, e -> enviarFrame());
                timerCamara.start();
                btn.setText("Detener Video");
            }
        } else {
            timerCamara.stop();
            manager.detenerCamara();
            btn.setText("Iniciar Video");
        }
    }

    private void enviarFrame() {
        BufferedImage frame = manager.capturarFrame();
        if (frame != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(frame, "jpg", baos);
                conexion.enviar(new MensajeSocket.Builder(MensajeSocket.CAMERA_FRAME)
                        .sala(roomCode)
                        .texto(Base64.getEncoder().encodeToString(baos.toByteArray()))
                        .build());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void enviarArchivo() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            manager.enviarArchivo(chooser.getSelectedFile(), roomCode, conexion);
        }
    }

    private void gestionarEspera(boolean aceptar) {
        String sel = listaEspera.getSelectedValue();
        if (sel == null) return;
        conexion.enviar(new MensajeSocket.Builder(MensajeSocket.ADMIT_USER)
                .sala(roomCode).usuario(mapaEspera.get(sel), sel).respuesta(aceptar, "").build());
        modeloEspera.removeElement(sel);
    }

    private void mostrarFrameRemoto(String u, String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            camarasRemotas.computeIfAbsent(u, k -> {
                JLabel l = new JLabel("Cámara " + u);
                panelCamaras.add(l);
                panelCamaras.revalidate();
                return l;
            }).setIcon(new ImageIcon(img));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void agregarAEspera(int id, String nombre) {
        if (esHost && !mapaEspera.containsKey(nombre)) {
            mapaEspera.put(nombre, id);
            modeloEspera.addElement(nombre);
        }
    }

    private JLabel camaraLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setPreferredSize(new Dimension(270, 180));
        l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        l.setOpaque(true);
        l.setBackground(Color.BLACK);
        l.setForeground(Color.WHITE);
        return l;
    }
}