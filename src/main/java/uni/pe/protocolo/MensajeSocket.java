package uni.pe.protocolo;

public class MensajeSocket {

    // Tipos de mensaje
    public static final String LOGIN_REQUEST       = "LOGIN_REQUEST";
    public static final String LOGIN_RESPONSE      = "LOGIN_RESPONSE";
    public static final String REGISTER_REQUEST    = "REGISTER_REQUEST";
    public static final String REGISTER_RESPONSE   = "REGISTER_RESPONSE";
    public static final String CREATE_ROOM         = "CREATE_ROOM";
    public static final String CREATE_ROOM_RESPONSE= "CREATE_ROOM_RESPONSE";
    public static final String JOIN_ROOM_REQUEST   = "JOIN_ROOM_REQUEST";
    public static final String WAITING_ROOM_UPDATE = "WAITING_ROOM_UPDATE";
    public static final String ADMIT_USER          = "ADMIT_USER";
    public static final String ADMIT_RESPONSE      = "ADMIT_RESPONSE";
    public static final String CHAT_MESSAGE        = "CHAT_MESSAGE";
    public static final String FILE_START          = "FILE_START";
    public static final String FILE_CHUNK          = "FILE_CHUNK";
    public static final String FILE_END            = "FILE_END";
    public static final String FILE_NOTIFY         = "FILE_NOTIFY";
    public static final String CAMERA_FRAME        = "CAMERA_FRAME";
    public static final String LEAVE_ROOM          = "LEAVE_ROOM";
    public static final String ERROR               = "ERROR";

    // Campos del mensaje
    private String type;
    private String correo;
    private String password;
    private String nombres;
    private int idUsuario;
    private String nombreUsuario;
    private String roomCode;
    private String roomName;
    private boolean exito;
    private String mensaje;
    private String contenido;
    private String nombreArchivo;
    private String chunkBase64;
    private int chunkIndex;
    private int totalChunks;
    private long tamanio;
    private String frameBase64;
    private String sentAt;
    private boolean aceptado;

    public MensajeSocket() {}

    // Getters y Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getChunkBase64() { return chunkBase64; }
    public void setChunkBase64(String chunkBase64) { this.chunkBase64 = chunkBase64; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public long getTamanio() { return tamanio; }
    public void setTamanio(long tamanio) { this.tamanio = tamanio; }
    public String getFrameBase64() { return frameBase64; }
    public void setFrameBase64(String frameBase64) { this.frameBase64 = frameBase64; }
    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
    public boolean isAceptado() { return aceptado; }
    public void setAceptado(boolean aceptado) { this.aceptado = aceptado; }
}