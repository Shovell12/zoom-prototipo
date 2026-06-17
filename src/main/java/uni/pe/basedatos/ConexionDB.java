package uni.pe.basedatos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    private static final String URL = "jdbc:sqlite:basedatos/zoom.db";
    private static Connection conexion = null;

    public static Connection getConexion() {
        if (conexion == null) {
            try {
                conexion = DriverManager.getConnection(URL);
                System.out.println("Base de datos conectada.");
                inicializarTablas();
            } catch (SQLException e) {
                System.err.println("Error al conectar BD: " + e.getMessage());
            }
        }
        return conexion;
    }

    private static void inicializarTablas() {
        String[] tablas = {
                """
            CREATE TABLE IF NOT EXISTS Usuarios (
                IdUsuario    INTEGER PRIMARY KEY AUTOINCREMENT,
                Nombres      TEXT NOT NULL,
                Correo       TEXT NOT NULL UNIQUE,
                PasswordHash TEXT NOT NULL,
                Rol          TEXT NOT NULL DEFAULT 'Usuario',
                Activo       INTEGER NOT NULL DEFAULT 1
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Salas (
                IdSala        INTEGER PRIMARY KEY AUTOINCREMENT,
                CodigoSala    TEXT NOT NULL UNIQUE,
                Nombre        TEXT NOT NULL,
                IdHost        INTEGER NOT NULL,
                Estado        TEXT NOT NULL DEFAULT 'Activa',
                FechaCreacion TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (IdHost) REFERENCES Usuarios(IdUsuario)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS ParticipantesSala (
                IdParticipante INTEGER PRIMARY KEY AUTOINCREMENT,
                IdSala         INTEGER NOT NULL,
                IdUsuario      INTEGER NOT NULL,
                Estado         TEXT NOT NULL,
                FechaIngreso   TEXT,
                FOREIGN KEY (IdSala) REFERENCES Salas(IdSala),
                FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Mensajes (
                IdMensaje  INTEGER PRIMARY KEY AUTOINCREMENT,
                IdSala     INTEGER NOT NULL,
                IdUsuario  INTEGER NOT NULL,
                Contenido  TEXT NOT NULL,
                EnviadoEn  TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (IdSala) REFERENCES Salas(IdSala),
                FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS ArchivosCompartidos (
                IdArchivo     INTEGER PRIMARY KEY AUTOINCREMENT,
                IdSala        INTEGER NOT NULL,
                IdUsuario     INTEGER NOT NULL,
                NombreArchivo TEXT NOT NULL,
                RutaServidor  TEXT NOT NULL,
                Tamanio       INTEGER,
                SubidoEn      TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (IdSala) REFERENCES Salas(IdSala),
                FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS SolicitudesSala (
                IdSolicitud    INTEGER PRIMARY KEY AUTOINCREMENT,
                IdSala         INTEGER NOT NULL,
                IdUsuario      INTEGER NOT NULL,
                Estado         TEXT NOT NULL DEFAULT 'Pendiente',
                FechaSolicitud TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (IdSala) REFERENCES Salas(IdSala),
                FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario)
            )
            """
        };

        try (Statement stmt = conexion.createStatement()) {
            for (String sql : tablas) {
                stmt.execute(sql);
            }
            System.out.println("Tablas inicializadas correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al crear tablas: " + e.getMessage());
        }
    }
}