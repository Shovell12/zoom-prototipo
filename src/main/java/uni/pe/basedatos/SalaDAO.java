package uni.pe.basedatos;

import uni.pe.modelo.Sala;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalaDAO {

    public static Sala crear(String codigoSala, String nombre, int idHost) {
        String sql = "INSERT INTO Salas (CodigoSala, Nombre, IdHost) VALUES (?, ?, ?)";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, codigoSala);
            ps.setString(2, nombre);
            ps.setInt(3, idHost);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                Sala s = new Sala(codigoSala, nombre, idHost);
                s.setIdSala(keys.getInt(1));
                return s;
            }
        } catch (SQLException e) {
            System.err.println("Error al crear sala: " + e.getMessage());
        }
        return null;
    }

    public static Sala buscarPorCodigo(String codigoSala) {
        String sql = "SELECT * FROM Salas WHERE CodigoSala = ? AND Estado = 'Activa'";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, codigoSala);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Sala s = new Sala();
                s.setIdSala(rs.getInt("IdSala"));
                s.setCodigoSala(rs.getString("CodigoSala"));
                s.setNombre(rs.getString("Nombre"));
                s.setIdHost(rs.getInt("IdHost"));
                s.setEstado(rs.getString("Estado"));
                return s;
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar sala: " + e.getMessage());
        }
        return null;
    }

    public static boolean registrarSolicitud(int idSala, int idUsuario) {
        String sql = "INSERT INTO SolicitudesSala (IdSala, IdUsuario, Estado) VALUES (?, ?, 'Pendiente')";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar solicitud: " + e.getMessage());
            return false;
        }
    }

    public static boolean admitirUsuario(int idSala, int idUsuario) {
        try {
            String check = "SELECT COUNT(*) FROM ParticipantesSala WHERE IdSala = ? AND IdUsuario = ?";
            try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(check)) {
                ps.setInt(1, idSala);
                ps.setInt(2, idUsuario);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) return true;
            }

            String upd = "UPDATE SolicitudesSala SET Estado = 'Aceptado' WHERE IdSala = ? AND IdUsuario = ?";
            try (PreparedStatement ps1 = ConexionDB.getConexion().prepareStatement(upd)) {
                ps1.setInt(1, idSala);
                ps1.setInt(2, idUsuario);
                ps1.executeUpdate();
            }

            String ins = "INSERT INTO ParticipantesSala (IdSala, IdUsuario, Estado, FechaIngreso) VALUES (?, ?, 'Aceptado', datetime('now'))";
            try (PreparedStatement ps2 = ConexionDB.getConexion().prepareStatement(ins)) {
                ps2.setInt(1, idSala);
                ps2.setInt(2, idUsuario);
                ps2.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error al admitir usuario: " + e.getMessage());
            return false;
        }
    }

    public static boolean rechazarUsuario(int idSala, int idUsuario) {
        String sql = "UPDATE SolicitudesSala SET Estado = 'Rechazado' WHERE IdSala = ? AND IdUsuario = ?";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al rechazar usuario: " + e.getMessage());
            return false;
        }
    }

    public static List<Integer> getParticipantesAceptados(int idSala) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT IdUsuario FROM ParticipantesSala WHERE IdSala = ? AND Estado = 'Aceptado'";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idSala);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("IdUsuario"));
        } catch (SQLException e) {
            System.err.println("Error al obtener participantes: " + e.getMessage());
        }
        return ids;
    }
}