package uni.pe.servidor;

import com.google.gson.Gson;
import uni.pe.basedatos.ConexionDB;
import uni.pe.protocolo.MensajeSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {

    private static final int PUERTO = 5000;
    private static final Map<Integer, ManejadorCliente> clientes = new ConcurrentHashMap<>();
    private static final Map<String, Set<Integer>> salas = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    private static String obtenerIpLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getAddress().length == 4) return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "desconocida";
    }

    public static void main(String[] args) {
        ConexionDB.getConexion();
        ExecutorService pool = Executors.newCachedThreadPool();
        String ip = obtenerIpLocal();
        System.out.println("Servidor iniciado — IP: " + ip + "  Puerto: " + PUERTO);

        try {
            ServerSocket servidor = new ServerSocket(PUERTO);

            Thread hiloAceptar = new Thread(() -> {
                try {
                    while (!servidor.isClosed()) {
                        Socket cliente = servidor.accept();
                        System.out.println("Nueva conexión: " + cliente.getInetAddress());
                        pool.execute(new ManejadorCliente(cliente));
                    }
                } catch (IOException e) {
                    if (!servidor.isClosed()) {
                        System.err.println("Error en servidor: " + e.getMessage());
                    }
                }
            });
            hiloAceptar.setDaemon(true);
            hiloAceptar.start();

            javax.swing.JFrame ventana = new javax.swing.JFrame("Servidor activo");
            ventana.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
            ventana.setSize(300, 160);
            ventana.setLocationRelativeTo(null);
            ventana.setLayout(new java.awt.BorderLayout(10, 10));

            javax.swing.JLabel etiqueta = new javax.swing.JLabel(
                    "<html><center>Servidor en ejecución.<br/>IP: <b>" + ip + "</b><br/>Puerto: <b>" + PUERTO + "</b></center></html>",
                    javax.swing.SwingConstants.CENTER);
            ventana.add(etiqueta, java.awt.BorderLayout.CENTER);

            javax.swing.JButton btnDetener = new javax.swing.JButton("Detener servidor");
            btnDetener.addActionListener(e -> {
                int confirmacion = javax.swing.JOptionPane.showConfirmDialog(ventana,
                        "¿Detener el servidor? Se desconectarán todos los clientes.",
                        "Confirmar", javax.swing.JOptionPane.YES_NO_OPTION);
                if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
                    try {
                        servidor.close();
                    } catch (IOException ex) {
                        System.err.println("Error al cerrar servidor: " + ex.getMessage());
                    }
                    pool.shutdownNow();
                    System.out.println("Servidor detenido.");
                    ventana.dispose();
                    System.exit(0);
                }
            });
            javax.swing.JPanel panel = new javax.swing.JPanel();
            panel.add(btnDetener);
            ventana.add(panel, java.awt.BorderLayout.SOUTH);
            ventana.setVisible(true);

        } catch (IOException e) {
            System.err.println("Error al iniciar servidor: " + e.getMessage());
        }
    }

    public static void registrarCliente(int idUsuario, ManejadorCliente manejador) {
        clientes.put(idUsuario, manejador);
    }

    public static void eliminarCliente(int idUsuario) {
        clientes.remove(idUsuario);
    }

    public static void unirseASala(String roomCode, int idUsuario) {
        salas.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(idUsuario);
    }

    public static void salirDeSala(String roomCode, int idUsuario) {
        Set<Integer> miembros = salas.get(roomCode);
        if (miembros != null) miembros.remove(idUsuario);
    }

    public static void enviarA(int idUsuario, MensajeSocket msg) {
        ManejadorCliente cliente = clientes.get(idUsuario);
        if (cliente != null) cliente.enviar(msg);
    }

    public static void broadcast(String roomCode, MensajeSocket msg, int excepto) {
        Set<Integer> miembros = salas.getOrDefault(roomCode, Collections.emptySet());
        for (int id : miembros) {
            if (id != excepto) enviarA(id, msg);
        }
    }
}