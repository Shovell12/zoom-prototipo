package uni.pe.basedatos;

import uni.pe.modelo.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensajeDAO {

    public static boolean guardar(int idSala, int idUsuario, String contenido) {
        String sql = "INSERT INTO Mensajes (IdSala, IdUsuario, Contenido) VALUES (?, ?, ?)";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            ps.setString(3, contenido);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al guardar mensaje: " + e.getMessage());
            return false;
        }
    }

    public static List<Mensaje> obtenerPorSala(int idSala) {
        List<Mensaje> lista = new ArrayList<>();
        String sql = """
            SELECT m.IdMensaje, m.IdSala, m.IdUsuario, u.Nombres, m.Contenido, m.EnviadoEn
            FROM Mensajes m
            JOIN Usuarios u ON m.IdUsuario = u.IdUsuario
            WHERE m.IdSala = ?
            ORDER BY m.EnviadoEn ASC
        """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Mensaje msg = new Mensaje();
                msg.setIdMensaje(rs.getInt("IdMensaje"));
                msg.setIdSala(rs.getInt("IdSala"));
                msg.setIdUsuario(rs.getInt("IdUsuario"));
                msg.setNombreUsuario(rs.getString("Nombres"));
                msg.setContenido(rs.getString("Contenido"));
                msg.setEnviadoEn(rs.getString("EnviadoEn"));
                lista.add(msg);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener mensajes: " + e.getMessage());
        }
        return lista;
    }
}