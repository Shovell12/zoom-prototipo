package uni.pe.basedatos;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionDB {

    private static final String URL = buildUrl();
    private static Connection conexion = null;

    private static String buildUrl() {
        try {
            URI uri = ConexionDB.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            File location = new File(uri);
            // Si estamos en clases compiladas (target/classes), subimos dos niveles al raíz del proyecto.
            // Si estamos en un JAR, usamos el directorio que contiene el JAR.
            File baseDir = location.isDirectory()
                    ? location.getParentFile().getParentFile()
                    : location.getParentFile();
            File dbDir = new File(baseDir, "basedatos");
            dbDir.mkdirs();
            return "jdbc:sqlite:" + new File(dbDir, "zoom.db").getAbsolutePath();
        } catch (Exception e) {
            return "jdbc:sqlite:basedatos/zoom.db";
        }
    }

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