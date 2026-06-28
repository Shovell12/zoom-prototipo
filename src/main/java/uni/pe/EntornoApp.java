package uni.pe;

import javax.swing.*;
import java.io.PrintStream;

class EntornoApp {

    private EntornoApp() {}

    static void configurar() {
        configurarEncoding();
        configurarHiDPI();
        configurarTema();
    }

    private static void configurarEncoding() {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception ignored) {}
    }

    private static void configurarHiDPI() {
        // Debe establecerse antes de que AWT inicialice
        System.setProperty("sun.java2d.uiScale", "true");
    }

    private static void configurarTema() {
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
            UIManager.put("Component.accentColor", new java.awt.Color(14, 113, 235));
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            System.err.println("No se pudo aplicar el tema FlatLaf: " + e.getMessage());
        }
    }
}
