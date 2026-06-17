package uni.pe.basedatos;

import org.mindrot.jbcrypt.BCrypt;
import uni.pe.modelo.Usuario;

import java.sql.*;

public class UsuarioDAO {

    public static boolean registrar(String nombres, String correo, String password) {
        String sql = "INSERT INTO Usuarios (Nombres, Correo, PasswordHash, Rol) VALUES (?, ?, ?, 'Usuario')";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, nombres);
            ps.setString(2, correo);
            ps.setString(3, hash);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    public static Usuario login(String correo, String password) {
        String sql = "SELECT * FROM Usuarios WHERE Correo = ? AND Activo = 1";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, correo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("PasswordHash");
                if (BCrypt.checkpw(password, hash)) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("IdUsuario"));
                    u.setNombres(rs.getString("Nombres"));
                    u.setCorreo(rs.getString("Correo"));
                    u.setRol(rs.getString("Rol"));
                    return u;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en login: " + e.getMessage());
        }
        return null;
    }

    public static Usuario buscarPorId(int idUsuario) {
        String sql = "SELECT * FROM Usuarios WHERE IdUsuario = ?";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("IdUsuario"));
                u.setNombres(rs.getString("Nombres"));
                u.setCorreo(rs.getString("Correo"));
                u.setRol(rs.getString("Rol"));
                return u;
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario: " + e.getMessage());
        }
        return null;
    }
}