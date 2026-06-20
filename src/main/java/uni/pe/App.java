package uni.pe;

import uni.pe.cliente.ConexionCliente;
import uni.pe.servidor.Servidor;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
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
            case 1 -> {
                String ip = JOptionPane.showInputDialog(
                        null,
                        "Ingresa la IP del servidor:",
                        "Conectar al servidor",
                        JOptionPane.PLAIN_MESSAGE);
                if (ip == null || ip.isBlank()) System.exit(0);
                SwingUtilities.invokeLater(() -> new ConexionCliente(ip.trim()).iniciar());
            }
            default -> System.exit(0);
        }
    }
}