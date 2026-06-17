package uni.pe.basedatos;

import java.sql.*;

public class ArchivoDAO {

    public static boolean registrar(int idSala, int idUsuario,
                                    String nombreArchivo, String rutaServidor, long tamanio) {
        String sql = """
            INSERT INTO ArchivosCompartidos (IdSala, IdUsuario, NombreArchivo, RutaServidor, Tamanio)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            ps.setString(3, nombreArchivo);
            ps.setString(4, rutaServidor);
            ps.setLong(5, tamanio);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar archivo: " + e.getMessage());
            return false;
        }
    }

    public static String obtenerRuta(String nombreArchivo, int idSala) {
        String sql = "SELECT RutaServidor FROM ArchivosCompartidos WHERE NombreArchivo = ? AND IdSala = ?";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, nombreArchivo);
            ps.setInt(2, idSala);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("RutaServidor");
        } catch (SQLException e) {
            System.err.println("Error al obtener ruta: " + e.getMessage());
        }
        return null;
    }
}