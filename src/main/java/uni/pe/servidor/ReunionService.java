package uni.pe.servidor;

import uni.pe.basedatos.MensajeDAO;
import uni.pe.basedatos.SalaDAO;
import uni.pe.basedatos.UsuarioDAO;
import uni.pe.modelo.Sala;
import uni.pe.modelo.Usuario;

/**
 * Capa de negocio entre el protocolo de red y los DAOs.
 * ManejadorCliente solo habla con esta clase y con Servidor; cero imports de DAO fuera de aquí.
 */
class ReunionService {

    Usuario login(String correo, String password) {
        return UsuarioDAO.login(correo, password);
    }

    boolean registrar(String nombres, String correo, String password) {
        return UsuarioDAO.registrar(nombres, correo, password);
    }

    /**
     * Crea la sala, admite al host y devuelve el código usado.
     * Retorna null si la creación falló.
     */
    String crearSala(String codigoSolicitado, String nombre, int idHost) {
        String codigo = codigoSolicitado != null
                ? codigoSolicitado
                : "SALA" + (int) (Math.random() * 9000 + 1000);
        Sala sala = SalaDAO.crear(codigo, nombre, idHost);
        if (sala == null) return null;
        SalaDAO.admitirUsuario(sala.getIdSala(), idHost);
        return codigo;
    }

    /**
     * Registra la solicitud de unión y retorna el idUsuario del host.
     * Retorna -1 si la sala no existe.
     */
    int solicitarUnirse(String roomCode, int idUsuario) {
        Sala sala = SalaDAO.buscarPorCodigo(roomCode);
        if (sala == null) return -1;
        SalaDAO.registrarSolicitud(sala.getIdSala(), idUsuario);
        return sala.getIdHost();
    }

    void admitirEnSala(String roomCode, int idUsuario) {
        Sala sala = SalaDAO.buscarPorCodigo(roomCode);
        if (sala != null) SalaDAO.admitirUsuario(sala.getIdSala(), idUsuario);
    }

    boolean esHost(String roomCode, int idUsuario) {
        Sala sala = SalaDAO.buscarPorCodigo(roomCode);
        return sala != null && sala.getIdHost() == idUsuario;
    }

    void guardarMensaje(String roomCode, int idUsuario, String contenido) {
        Sala sala = SalaDAO.buscarPorCodigo(roomCode);
        if (sala != null) MensajeDAO.guardar(sala.getIdSala(), idUsuario, contenido);
    }
}
