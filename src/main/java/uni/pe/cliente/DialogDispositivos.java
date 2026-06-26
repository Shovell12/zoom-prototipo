package uni.pe.cliente;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DialogDispositivos extends JDialog {

    private static final String POR_DEFECTO = "Sistema por defecto";

    private final JComboBox<String> comboMic;
    private final JComboBox<String> comboSalida;
    private final List<Mixer.Info>  microfonos;
    private final List<Mixer.Info>  salidas;

    private boolean aplicado = false;

    public DialogDispositivos(Window owner) {
        super(owner, "Dispositivos de audio", ModalityType.APPLICATION_MODAL);
        setSize(440, 230);
        setMinimumSize(new Dimension(380, 210));
        setLocationRelativeTo(owner);
        setResizable(false);

        microfonos = MicrofoneCaptura.listarMicrofonos();
        salidas    = MicrofoneCaptura.listarSalidaAudio();

        comboMic    = new JComboBox<>();
        comboSalida = new JComboBox<>();
        poblarCombos();

        setContentPane(buildPanel());
    }

    private void poblarCombos() {
        comboMic.addItem(POR_DEFECTO);
        for (Mixer.Info mi : microfonos) comboMic.addItem(mi.getName());

        comboSalida.addItem(POR_DEFECTO);
        for (Mixer.Info mi : salidas) comboSalida.addItem(mi.getName());

        // Pre-seleccionar preferencia guardada
        Mixer.Info micGuardado   = PreferenciasAudio.getMicrofono();
        Mixer.Info salidaGuardada = PreferenciasAudio.getSalida();

        if (micGuardado != null) {
            for (int i = 0; i < microfonos.size(); i++) {
                if (microfonos.get(i).getName().equals(micGuardado.getName())) {
                    comboMic.setSelectedIndex(i + 1);
                    break;
                }
            }
        }
        if (salidaGuardada != null) {
            for (int i = 0; i < salidas.size(); i++) {
                if (salidas.get(i).getName().equals(salidaGuardada.getName())) {
                    comboSalida.setSelectedIndex(i + 1);
                    break;
                }
            }
        }
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(ReunionTheme.BG_TOOLBAR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(6, 0, 6, 12);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;

        JLabel lblMic    = label("Micrófono (entrada):");
        JLabel lblSalida = label("Altavoz (salida):");

        styleCombo(comboMic);
        styleCombo(comboSalida);

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; form.add(lblMic, gc);
        gc.gridx = 1; gc.weightx = 1; form.add(comboMic, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; form.add(lblSalida, gc);
        gc.gridx = 1; gc.weightx = 1; form.add(comboSalida, gc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton btnCancelar = boton("Cancelar", false);
        JButton btnAplicar  = boton("Aplicar",  true);

        btnCancelar.addActionListener(e -> dispose());
        btnAplicar.addActionListener(e -> aplicar());

        botones.add(btnCancelar);
        botones.add(btnAplicar);

        panel.add(form,    BorderLayout.CENTER);
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    private void aplicar() {
        int micIdx    = comboMic.getSelectedIndex();
        int salidaIdx = comboSalida.getSelectedIndex();

        PreferenciasAudio.setMicrofono(micIdx    <= 0 ? null : microfonos.get(micIdx    - 1));
        PreferenciasAudio.setSalida(  salidaIdx  <= 0 ? null : salidas   .get(salidaIdx - 1));

        aplicado = true;
        dispose();
    }

    /** true si el usuario pulsó Aplicar (y no sólo cerró el diálogo) */
    public boolean isAplicado() { return aplicado; }

    // ── Helpers de estilo ─────────────────────────────────────────────────────

    private JLabel label(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setForeground(ReunionTheme.TEXT_WHITE);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return lbl;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(ReunionTheme.COMBO_BG);
        combo.setForeground(ReunionTheme.TEXT_WHITE);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(260, 28));
    }

    private JButton boton(String texto, boolean primario) {
        JButton btn = new JButton(texto);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primario) {
            btn.setBackground(ReunionTheme.ZOOM_BLUE);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(ReunionTheme.BTN_SECONDARY_BG);
            btn.setForeground(ReunionTheme.TEXT_GRAY);
        }
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(90, 30));
        return btn;
    }
}
