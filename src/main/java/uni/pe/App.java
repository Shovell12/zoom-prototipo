package uni.pe;

import uni.pe.cliente.ConexionCliente;
import uni.pe.cliente.VentanaLogin;
import uni.pe.servidor.Servidor;

import javax.swing.*;

public class App {

    public static void main(String[] args) {
        if (WindowsRelaunch.necesario() && WindowsRelaunch.ejecutar(args)) return;
        EntornoApp.configurar();
        mostrarSelectorModo(args);
    }

    private static void mostrarSelectorModo(String[] args) {
        String[] opciones = {"Servidor", "Cliente"};
        int eleccion = JOptionPane.showOptionDialog(
                null,
                "¿Cómo deseas iniciar la aplicación?",
                "Zoom Prototipo",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        switch (eleccion) {
            case 0 -> Servidor.main(args);
            case 1 -> iniciarCliente();
            default -> System.exit(0);
        }
    }

    private static void iniciarCliente() {
        String ip = JOptionPane.showInputDialog(
                null,
                "Ingresa la IP del servidor:",
                "Conectar al servidor",
                JOptionPane.PLAIN_MESSAGE);

        if (ip == null || ip.isBlank()) { System.exit(0); return; }

        SwingUtilities.invokeLater(() -> {
            try {
                new VentanaLogin(new ConexionCliente(ip.trim(), Servidor.PUERTO)).setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo conectar al servidor:\n" + e.getCause().getMessage(),
                        "Error de conexión", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
