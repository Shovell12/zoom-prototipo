package uni.pe.cliente;

import java.util.Collections;
import java.util.List;

/**
 * Configuración inmutable de una sala de reunión.
 * Usar {@link Builder} para construir instancias.
 */
public final class ReunionConfig {

    public final ConexionCliente conexion;
    public final int             idUsuario;
    public final String          nombreUsuario;
    public final String          roomCode;
    public final boolean         esHost;
    public final List<String>    participantesIniciales;

    private ReunionConfig(Builder b) {
        this.conexion               = b.conexion;
        this.idUsuario              = b.idUsuario;
        this.nombreUsuario          = b.nombreUsuario;
        this.roomCode               = b.roomCode;
        this.esHost                 = b.esHost;
        this.participantesIniciales = Collections.unmodifiableList(b.participantesIniciales);
    }

    public static final class Builder {
        private final ConexionCliente conexion;
        private final int             idUsuario;
        private final String          nombreUsuario;
        private final String          roomCode;
        private boolean       esHost                 = false;
        private List<String>  participantesIniciales = Collections.emptyList();

        public Builder(ConexionCliente conexion, int idUsuario,
                       String nombreUsuario, String roomCode) {
            this.conexion      = conexion;
            this.idUsuario     = idUsuario;
            this.nombreUsuario = nombreUsuario;
            this.roomCode      = roomCode;
        }

        public Builder esHost(boolean esHost) {
            this.esHost = esHost;
            return this;
        }

        public Builder participantes(List<String> lista) {
            this.participantesIniciales = lista;
            return this;
        }

        public ReunionConfig build() {
            return new ReunionConfig(this);
        }
    }
}
