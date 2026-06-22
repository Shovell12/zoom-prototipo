package uni.pe.cliente;

import uni.pe.protocolo.MensajeSocket;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class VentanaReunion extends JFrame {

    private final ConexionCliente conexion;
    private final int idUsuario;
    private final String nombreUsuario;
    private final String roomCode;
    private final boolean esHost;

    // Chat
    private JTextArea areaChat;
    private JTextField txtMensaje;

    // Participantes en espera (solo host)
    private DefaultListModel<String> modeloEspera;
    private JList<String> listaEspera;
    private final Map<String, Integer> mapaEspera = new HashMap<>();

    // Cámara
    private JLabel lblCamaraLocal;
    private JLabel lblCamaraRemota;
    private javax.swing.Timer timerCamara;
    private uni.pe.cliente.CamaraCaptura camara;

    // Micrófono (captura)
    private MicrofoneCaptura microfono;
    private Thread hiloMicrofono;
    private volatile boolean micActivo = false;

    // Audio (reproducción)
    private SourceDataLine lineaAudio;
    private Thread hiloReproduccion;
    private final LinkedBlockingQueue<byte[]> colaAudio = new LinkedBlockingQueue<>();
    private Mixer.Info mixerSalida = null;

    // Archivos
    private JList<String> listaArchivos;
    private DefaultListModel<String> modeloArchivos;

    public VentanaReunion(ConexionCliente conexion, int idUsuario,
                          String nombreUsuario, String roomCode, boolean esHost) {
        this.conexion = conexion;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.roomCode = roomCode;
        this.esHost = esHost;
        iniciarUI();
    }

    private void iniciarUI() {
        setTitle("Sala: " + roomCode + (esHost ? " [HOST]" : ""));
        setSize(900, 650);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel principal
        JPanel panelPrincipal = new JPanel(new BorderLayout(5, 5));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Panel izquierdo: cámaras + espera ──
        JPanel panelIzq = new JPanel(new BorderLayout(5, 5));
        panelIzq.setPreferredSize(new Dimension(280, 0));

        lblCamaraLocal  = camaraLabel("Tu cámara");
        lblCamaraRemota = camaraLabel("Cámara remota");

        JPanel panelCamaras = new JPanel(new GridLayout(2, 1, 5, 5));
        panelCamaras.add(lblCamaraLocal);
        panelCamaras.add(lblCamaraRemota);
        panelIzq.add(panelCamaras, BorderLayout.CENTER);

        // Panel sala de espera (solo host)
        if (esHost) {
            modeloEspera = new DefaultListModel<>();
            listaEspera  = new JList<>(modeloEspera);
            JPanel panelEspera = new JPanel(new BorderLayout(3, 3));
            panelEspera.setBorder(BorderFactory.createTitledBorder("Sala de espera"));
            panelEspera.setPreferredSize(new Dimension(280, 160));
            panelEspera.add(new JScrollPane(listaEspera), BorderLayout.CENTER);

            JPanel botonesEspera = new JPanel(new GridLayout(1, 2, 4, 0));
            JButton btnAceptar  = new JButton("Aceptar");
            JButton btnRechazar = new JButton("Rechazar");
            botonesEspera.add(btnAceptar);
            botonesEspera.add(btnRechazar);
            panelEspera.add(botonesEspera, BorderLayout.SOUTH);
            panelIzq.add(panelEspera, BorderLayout.SOUTH);

            btnAceptar.addActionListener(e -> admitir(true));
            btnRechazar.addActionListener(e -> admitir(false));
        }

        // ── Panel central: chat ──
        JPanel panelChat = new JPanel(new BorderLayout(5, 5));
        panelChat.setBorder(BorderFactory.createTitledBorder("Chat — " + roomCode));

        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true);
        panelChat.add(new JScrollPane(areaChat), BorderLayout.CENTER);

        JPanel panelEnvio = new JPanel(new BorderLayout(4, 0));
        txtMensaje = new JTextField();
        JButton btnEnviar = new JButton("Enviar");
        panelEnvio.add(txtMensaje, BorderLayout.CENTER);
        panelEnvio.add(btnEnviar, BorderLayout.EAST);
        panelChat.add(panelEnvio, BorderLayout.SOUTH);

        // ── Panel derecho: archivos + controles ──
        JPanel panelDer = new JPanel(new BorderLayout(5, 5));
        panelDer.setPreferredSize(new Dimension(200, 0));

        modeloArchivos = new DefaultListModel<>();
        listaArchivos  = new JList<>(modeloArchivos);
        JPanel panelArchivos = new JPanel(new BorderLayout(3, 3));
        panelArchivos.setBorder(BorderFactory.createTitledBorder("Archivos compartidos"));
        panelArchivos.add(new JScrollPane(listaArchivos), BorderLayout.CENTER);

        listaArchivos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) descargarArchivo();
            }
        });

        JButton btnEnviarArchivo = new JButton("Enviar archivo");
        panelArchivos.add(btnEnviarArchivo, BorderLayout.SOUTH);
        panelDer.add(panelArchivos, BorderLayout.CENTER);

        JPanel panelControles = new JPanel(new GridLayout(4, 1, 4, 4));
        JButton btnCamara        = new JButton("Activar cámara");
        JButton btnMicrofono     = new JButton("Activar micrófono");
        JButton btnSalidaAudio   = new JButton("Salida de audio");
        JButton btnSalir         = new JButton("Salir de sala");
        btnSalir.setBackground(new Color(200, 50, 50));
        btnSalir.setForeground(Color.WHITE);
        panelControles.add(btnCamara);
        panelControles.add(btnMicrofono);
        panelControles.add(btnSalidaAudio);
        panelControles.add(btnSalir);
        panelDer.add(panelControles, BorderLayout.SOUTH);

        // Ensamblar
        panelPrincipal.add(panelIzq,   BorderLayout.WEST);
        panelPrincipal.add(panelChat,  BorderLayout.CENTER);
        panelPrincipal.add(panelDer,   BorderLayout.EAST);
        add(panelPrincipal);

        // Acciones
        btnEnviar.addActionListener(e -> enviarMensaje());
        txtMensaje.addActionListener(e -> enviarMensaje());
        btnEnviarArchivo.addActionListener(e -> enviarArchivo());
        btnCamara.addActionListener(e -> toggleCamara(btnCamara));
        btnMicrofono.addActionListener(e -> toggleMicrofono(btnMicrofono));
        btnSalidaAudio.addActionListener(e -> seleccionarSalidaAudio());
        btnSalir.addActionListener(e -> salir());

        iniciarReproduccionAudio();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) { salir(); }
        });
    }

    // ── CHAT ──────────────────────────────────────────────────────────────────
    private void enviarMensaje() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty()) return;
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.CHAT_MESSAGE);
        msg.setRoomCode(roomCode);
        msg.setIdUsuario(idUsuario);
        msg.setNombreUsuario(nombreUsuario);
        msg.setContenido(texto);
        conexion.enviar(msg);
        agregarMensajeChat("Yo: " + texto);
        txtMensaje.setText("");
    }

    public void agregarMensajeChat(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaChat.append(texto + "\n");
            areaChat.setCaretPosition(areaChat.getDocument().getLength());
        });
    }

    // ── SALA DE ESPERA ────────────────────────────────────────────────────────
    public void agregarAEspera(int idUsr, String nombre) {
        SwingUtilities.invokeLater(() -> {
            if (modeloEspera != null && !mapaEspera.containsKey(nombre)) {
                mapaEspera.put(nombre, idUsr);
                modeloEspera.addElement(nombre);
            }
        });
    }

    private void admitir(boolean aceptar) {
        String seleccionado = listaEspera.getSelectedValue();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario de la lista.");
            return;
        }
        int idUsr = mapaEspera.get(seleccionado);
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.ADMIT_USER);
        msg.setRoomCode(roomCode);
        msg.setIdUsuario(idUsr);
        msg.setAceptado(aceptar);
        conexion.enviar(msg);
        mapaEspera.remove(seleccionado);
        modeloEspera.removeElement(seleccionado);
    }

    // ── ARCHIVOS ──────────────────────────────────────────────────────────────
    private void enviarArchivo() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File archivo = chooser.getSelectedFile();
        new Thread(() -> {
            try {
                byte[] datos = Files.readAllBytes(archivo.toPath());
                int CHUNK = 4096;
                int total = (int) Math.ceil((double) datos.length / CHUNK);

                MensajeSocket start = new MensajeSocket();
                start.setType(MensajeSocket.FILE_START);
                start.setNombreArchivo(archivo.getName());
                start.setTamanio(datos.length);
                start.setRoomCode(roomCode);
                conexion.enviar(start);

                for (int i = 0; i < total; i++) {
                    int desde = i * CHUNK;
                    int hasta = Math.min(desde + CHUNK, datos.length);
                    byte[] chunk = new byte[hasta - desde];
                    System.arraycopy(datos, desde, chunk, 0, chunk.length);

                    MensajeSocket chunkMsg = new MensajeSocket();
                    chunkMsg.setType(MensajeSocket.FILE_CHUNK);
                    chunkMsg.setNombreArchivo(archivo.getName());
                    chunkMsg.setChunkBase64(Base64.getEncoder().encodeToString(chunk));
                    chunkMsg.setChunkIndex(i);
                    chunkMsg.setTotalChunks(total);
                    chunkMsg.setRoomCode(roomCode);
                    conexion.enviar(chunkMsg);
                }

                MensajeSocket end = new MensajeSocket();
                end.setType(MensajeSocket.FILE_END);
                end.setNombreArchivo(archivo.getName());
                end.setRoomCode(roomCode);
                conexion.enviar(end);

                SwingUtilities.invokeLater(() ->
                        agregarMensajeChat("✓ Archivo enviado: " + archivo.getName()));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al enviar archivo: " + e.getMessage());
            }
        }).start();
    }

    public void agregarArchivo(String nombre) {
        SwingUtilities.invokeLater(() -> modeloArchivos.addElement(nombre));
    }

    private void descargarArchivo() {
        String nombre = listaArchivos.getSelectedValue();
        if (nombre == null) return;
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.FILE_DOWNLOAD_REQUEST);
        msg.setNombreArchivo(nombre);
        msg.setRoomCode(roomCode);
        conexion.enviar(msg);
    }

    public void guardarDescarga(String nombre, byte[] datos) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(nombre));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.write(chooser.getSelectedFile().toPath(), datos);
            agregarMensajeChat("Archivo descargado: " + nombre);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
        }
    }

    // ── CÁMARA ────────────────────────────────────────────────────────────────
    private void toggleCamara(JButton btn) {
        if (timerCamara == null || !timerCamara.isRunning()) {
            camara = new CamaraCaptura();
            if (!camara.iniciar()) {
                JOptionPane.showMessageDialog(this, "No se encontró cámara.");
                return;
            }
            timerCamara = new javax.swing.Timer(200, e -> capturarYEnviar());
            timerCamara.start();
            btn.setText("Detener cámara");
        } else {
            timerCamara.stop();
            if (camara != null) camara.detener();
            lblCamaraLocal.setIcon(null);
            lblCamaraLocal.setText("Tu cámara");
            btn.setText("Activar cámara");
        }
    }

    private void capturarYEnviar() {
        if (camara == null) return;
        BufferedImage frame = camara.capturarFrame();
        if (frame == null) return;

        // Mostrar local
        Image scaled = frame.getScaledInstance(
                lblCamaraLocal.getWidth(), lblCamaraLocal.getHeight(), Image.SCALE_FAST);
        lblCamaraLocal.setIcon(new ImageIcon(scaled));
        lblCamaraLocal.setText("");

        // Enviar al servidor
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(frame, "jpg", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            MensajeSocket msg = new MensajeSocket();
            msg.setType(MensajeSocket.CAMERA_FRAME);
            msg.setRoomCode(roomCode);
            msg.setFrameBase64(base64);
            conexion.enviar(msg);
        } catch (IOException e) {
            System.err.println("Error al capturar frame: " + e.getMessage());
        }
    }

    public void mostrarFrameRemoto(String base64) {
        SwingUtilities.invokeLater(() -> {
            try {
                byte[] datos = Base64.getDecoder().decode(base64);
                BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(datos));
                if (img != null) {
                    Image scaled = img.getScaledInstance(
                            lblCamaraRemota.getWidth(), lblCamaraRemota.getHeight(), Image.SCALE_FAST);
                    lblCamaraRemota.setIcon(new ImageIcon(scaled));
                    lblCamaraRemota.setText("");
                }
            } catch (IOException e) {
                System.err.println("Error al mostrar frame: " + e.getMessage());
            }
        });
    }

    // ── MICRÓFONO ─────────────────────────────────────────────────────────────
    private void toggleMicrofono(JButton btn) {
        if (!micActivo) {
            List<Mixer.Info> mics = MicrofoneCaptura.listarMicrofonos();
            if (mics.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron micrófonos disponibles.");
                return;
            }

            Mixer.Info seleccionado;
            if (mics.size() == 1) {
                seleccionado = mics.get(0);
            } else {
                DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
                for (Mixer.Info mi : mics) modelo.addElement(mi.getName());
                JComboBox<String> combo = new JComboBox<>(modelo);
                int res = JOptionPane.showConfirmDialog(this, combo,
                        "Selecciona un micrófono", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) return;
                seleccionado = mics.get(combo.getSelectedIndex());
            }

            microfono = new MicrofoneCaptura();
            if (!microfono.iniciar(seleccionado)) {
                JOptionPane.showMessageDialog(this, "No se pudo abrir el micrófono seleccionado.");
                return;
            }
            micActivo = true;
            hiloMicrofono = new Thread(() -> {
                while (micActivo) {
                    byte[] chunk = microfono.capturarChunk();
                    if (chunk != null) {
                        MensajeSocket msg = new MensajeSocket();
                        msg.setType(MensajeSocket.AUDIO_FRAME);
                        msg.setRoomCode(roomCode);
                        msg.setAudioBase64(Base64.getEncoder().encodeToString(chunk));
                        conexion.enviar(msg);
                    }
                }
            });
            hiloMicrofono.setDaemon(true);
            hiloMicrofono.start();
            btn.setText("Silenciar");
        } else {
            micActivo = false;
            if (microfono != null) microfono.detener();
            btn.setText("Activar micrófono");
        }
    }

    private void iniciarReproduccionAudio() {
        if (hiloReproduccion != null) hiloReproduccion.interrupt();
        if (lineaAudio != null) { lineaAudio.drain(); lineaAudio.close(); }
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, MicrofoneCaptura.FORMATO);
            if (mixerSalida != null) {
                lineaAudio = (SourceDataLine) AudioSystem.getMixer(mixerSalida).getLine(info);
            } else {
                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Reproducción de audio no soportada.");
                    return;
                }
                lineaAudio = (SourceDataLine) AudioSystem.getLine(info);
            }
            lineaAudio.open(MicrofoneCaptura.FORMATO);
            lineaAudio.start();
            hiloReproduccion = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] datos = colaAudio.take();
                        lineaAudio.write(datos, 0, datos.length);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            hiloReproduccion.setDaemon(true);
            hiloReproduccion.start();
        } catch (LineUnavailableException e) {
            System.err.println("No se pudo iniciar reproductor de audio: " + e.getMessage());
        }
    }

    private void seleccionarSalidaAudio() {
        List<Mixer.Info> dispositivos = MicrofoneCaptura.listarSalidaAudio();
        if (dispositivos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron dispositivos de salida de audio.");
            return;
        }
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        for (Mixer.Info mi : dispositivos) modelo.addElement(mi.getName());
        JComboBox<String> combo = new JComboBox<>(modelo);
        if (mixerSalida != null) {
            for (int i = 0; i < dispositivos.size(); i++) {
                if (dispositivos.get(i).getName().equals(mixerSalida.getName())) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }
        int res = JOptionPane.showConfirmDialog(this, combo,
                "Selecciona dispositivo de salida", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        mixerSalida = dispositivos.get(combo.getSelectedIndex());
        iniciarReproduccionAudio();
    }

    public void reproducirAudio(String base64) {
        byte[] datos = Base64.getDecoder().decode(base64);
        colaAudio.offer(datos);
    }

    // ── SALIR ─────────────────────────────────────────────────────────────────
    private void salir() {
        if (timerCamara != null) timerCamara.stop();
        if (camara != null) camara.detener();
        micActivo = false;
        if (microfono != null) microfono.detener();
        if (hiloReproduccion != null) hiloReproduccion.interrupt();
        if (lineaAudio != null) { lineaAudio.drain(); lineaAudio.close(); }
        MensajeSocket msg = new MensajeSocket();
        msg.setType(MensajeSocket.LEAVE_ROOM);
        msg.setRoomCode(roomCode);
        conexion.enviar(msg);
        dispose();
        conexion.volverAlInicio();
    }

    // ── UTILIDAD ──────────────────────────────────────────────────────────────
    private JLabel camaraLabel(String texto) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(270, 180));
        lbl.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        lbl.setBackground(Color.BLACK);
        lbl.setForeground(Color.WHITE);
        lbl.setOpaque(true);
        return lbl;
    }
}