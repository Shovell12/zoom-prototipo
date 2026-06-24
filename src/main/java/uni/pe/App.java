package uni.pe;

import uni.pe.cliente.ConexionCliente;
import uni.pe.servidor.Servidor;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class App {
    public static void main(String[] args) {
        // En Windows, el JAR se auto-relanza añadiendo los flags necesarios para
        // bridj (backend nativo de webcam-capture) bajo Java 9+ con JPMS.
        if (isWindows() && System.getProperty("app.relaunched") == null) {
            if (relaunch(args)) return;
        }

        // HiDPI — debe establecerse antes de que AWT inicialice
        System.setProperty("sun.java2d.uiScale", "true");

        // Salida de consola en UTF-8 (evita caracteres corruptos en Windows CMD)
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception ignored) {}

        // Look and Feel moderno con FlatLaf (Tema Oscuro)
        // Look and Feel moderno con FlatLaf (Tema Oscuro)
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();

            // Personalización de colores estilo Zoom
            UIManager.put("Component.accentColor", new java.awt.Color(14, 113, 235));
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            System.err.println("No se pudo aplicar el tema FlatLaf: " + e.getMessage());
        }

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

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Relanza el JAR actual añadiendo los flags de JVM que bridj necesita bajo JPMS
     * (Java 9+). Detecta automáticamente el ejecutable java/javaw con el que se
     * inició el proceso para respetar el modo consola vs. sin consola.
     * Retorna true si el relanzamiento fue exitoso (el llamador debe salir).
     */
    private static boolean relaunch(String[] args) {
        try {
            URI uri = App.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File jar = new File(uri);
            if (!jar.getName().endsWith(".jar")) return false; // IDE (clases sueltas)

            String javaExe = ProcessHandle.current().info().command().orElse("java");

            List<String> cmd = new ArrayList<>(List.of(
                javaExe,
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/sun.misc=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/java.nio=ALL-UNNAMED",
                "-Dapp.relaunched=true",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dsun.java2d.uiScale=true",
                "-jar", jar.getAbsolutePath()
            ));
            cmd.addAll(Arrays.asList(args));

            new ProcessBuilder(cmd).inheritIO().start().waitFor();
            return true;
        } catch (Exception e) {
            return false; // si falla el relanzamiento, continúa sin los flags
        }
    }
}
