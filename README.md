# Zoom Prototipo

Aplicación de videoconferencia en tiempo real inspirada en Zoom, desarrollada en Java con arquitectura cliente-servidor sobre sockets TCP. Permite crear y unirse a reuniones virtuales con video, audio, chat y transferencia de archivos.

## Características

- **Video en tiempo real** — transmisión de cámara web entre participantes
- **Audio en tiempo real** — captura y envío de micrófono
- **Compartir pantalla** — difusión del escritorio a todos los participantes
- **Chat de sala** — mensajes de texto durante la reunión con historial persistente
- **Transferencia de archivos** — envío de archivos por chunks con notificaciones de descarga
- **Sala de espera** — el anfitrión admite o rechaza participantes antes de entrar
- **Gestión de salas** — códigos de sala únicos, creación y cierre controlado por el anfitrión
- **Autenticación** — registro e inicio de sesión con contraseñas cifradas con BCrypt
- **Interfaz moderna** — UI con tema oscuro usando FlatLaf

## Tecnologías

| Componente        | Tecnología                          |
|-------------------|-------------------------------------|
| Lenguaje          | Java 21                             |
| UI                | Java Swing + FlatLaf 3.5            |
| Base de datos     | SQLite (sqlite-jdbc 3.45)           |
| Protocolo de red  | Sockets TCP + JSON (Gson 2.10)      |
| Cámara web        | webcam-capture 0.3.12               |
| Seguridad         | BCrypt (jbcrypt 0.4)                |
| Build             | Maven 3                             |

## Arquitectura

```
zoom-prototipo/
├── src/main/java/uni/pe/
│   ├── App.java                  # Punto de entrada (elige modo servidor/cliente)
│   ├── servidor/                 # Lógica del servidor (Servidor, ManejadorCliente)
│   ├── cliente/                  # UI y lógica del cliente (ventanas, media, eventos)
│   ├── protocolo/                # Protocolo de mensajes (MensajeSocket con Builder)
│   ├── basedatos/                # DAOs y conexión SQLite
│   └── modelo/                   # Entidades (Usuario, Sala, Mensaje)
├── basedatos/zoom.db             # Base de datos SQLite
├── run.sh                        # Script de ejecución Linux/macOS
└── run.bat                       # Script de ejecución Windows
```

**Patrones de diseño aplicados:** Singleton (ConexionDB), Builder (MensajeSocket), Observer (ReunionEventBus), Command, Facade (ReunionManager), SRP con composición de paneles UI.

## Requisitos

- Java 21 o superior
- Maven 3.6 o superior

## Compilación

```bash
mvn package
```

Genera `target/zoom-prototipo-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Ejecución

**Linux / macOS:**
```bash
./run.sh
```

**Windows:**
```bat
run.bat
```

**Manual:**
```bash
java -Dfile.encoding=UTF-8 -jar target/zoom-prototipo-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Al iniciar, el diálogo pregunta si deseas arrancar como **Servidor** o **Cliente**.

## Uso

### Modo Servidor
1. Selecciona **Servidor** al iniciar.
2. La ventana del servidor muestra la IP local y el puerto (`5000`).
3. Los clientes se conectan a esa IP.

### Modo Cliente
1. Selecciona **Cliente** al iniciar e ingresa la IP del servidor.
2. Regístrate o inicia sesión.
3. Crea una sala (obtendrás un código) o únete con el código de otro anfitrión.
4. Una vez en la reunión:
   - Activa/desactiva cámara y micrófono con los botones de la barra de herramientas.
   - Comparte tu pantalla con el botón correspondiente.
   - Abre el chat o el gestor de archivos desde la barra.

## Puerto por defecto

El servidor escucha en el puerto **5000** TCP. Asegúrate de que ese puerto esté abierto en el firewall si los clientes se conectan desde otra red.
