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
            case 1 -> SwingUtilities.invokeLater(() -> new ConexionCliente().iniciar());
            default -> System.exit(0);
        }
    }
}