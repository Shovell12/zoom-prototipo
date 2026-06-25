package uni.pe.protocolo;

public class MensajeSocket {
    // [Constantes igual que antes]
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST", LOGIN_RESPONSE = "LOGIN_RESPONSE",
            REGISTER_REQUEST = "REGISTER_REQUEST", REGISTER_RESPONSE = "REGISTER_RESPONSE",
            CREATE_ROOM = "CREATE_ROOM", CREATE_ROOM_RESPONSE = "CREATE_ROOM_RESPONSE",
            JOIN_ROOM_REQUEST = "JOIN_ROOM_REQUEST", WAITING_ROOM_UPDATE = "WAITING_ROOM_UPDATE",
            ADMIT_USER = "ADMIT_USER", ADMIT_RESPONSE = "ADMIT_RESPONSE", CHAT_MESSAGE = "CHAT_MESSAGE",
            FILE_START = "FILE_START", FILE_CHUNK = "FILE_CHUNK", FILE_END = "FILE_END",
            FILE_NOTIFY = "FILE_NOTIFY", FILE_DOWNLOAD_REQUEST = "FILE_DOWNLOAD_REQUEST",
            CAMERA_FRAME = "CAMERA_FRAME", AUDIO_FRAME = "AUDIO_FRAME", LEAVE_ROOM = "LEAVE_ROOM", ERROR = "ERROR";

    private String type, correo, password, nombres, nombreUsuario, roomCode, roomName, mensaje, contenido, nombreArchivo, chunkBase64, frameBase64, audioBase64;
    private int idUsuario, chunkIndex, totalChunks;
    private long tamanio;
    private boolean exito, aceptado;

    public MensajeSocket() {}

    // Getters y Setters estándar
    public String getType() { return type; } public void setType(String t) { type = t; }
    public void setCorreo(String c) { correo = c; } public String getCorreo() { return correo; }
    public void setPassword(String p) { password = p; } public String getPassword() { return password; }
    public void setNombres(String n) { nombres = n; } public String getNombres() { return nombres; }
    public void setIdUsuario(int i) { idUsuario = i; } public int getIdUsuario() { return idUsuario; }
    public void setNombreUsuario(String n) { nombreUsuario = n; } public String getNombreUsuario() { return nombreUsuario; }
    public void setRoomCode(String r) { roomCode = r; } public String getRoomCode() { return roomCode; }
    public void setRoomName(String r) { roomName = r; } public String getRoomName() { return roomName; }
    public void setExito(boolean e) { exito = e; } public boolean isExito() { return exito; }
    public void setMensaje(String m) { mensaje = m; } public String getMensaje() { return mensaje; }
    public void setContenido(String c) { contenido = c; } public String getContenido() { return contenido; }
    public String getNombreArchivo() { return nombreArchivo; } public void setNombreArchivo(String n) { nombreArchivo = n; }
    public void setChunkBase64(String c) { chunkBase64 = c; } public String getChunkBase64() { return chunkBase64; }
    public void setChunkIndex(int i) { chunkIndex = i; } public void setTotalChunks(int t) { totalChunks = t; }
    public void setTamanio(long t) { tamanio = t; } public void setFrameBase64(String f) { frameBase64 = f; }
    public void setAudioBase64(String a) { audioBase64 = a; } public void setAceptado(boolean a) { aceptado = a; }
    public boolean isAceptado() { return aceptado; }

    public static class Builder {
        private final MensajeSocket msg;
        public Builder(String type) { msg = new MensajeSocket(); msg.setType(type); }
        public Builder usuario(int id, String nombre) { msg.setIdUsuario(id); msg.setNombreUsuario(nombre); return this; }
        public Builder sala(String code) { msg.setRoomCode(code); return this; }
        public Builder sala(String code, String name) { msg.setRoomCode(code); msg.setRoomName(name); return this; }
        public Builder respuesta(boolean exito, String mensaje) { msg.setExito(exito); msg.setMensaje(mensaje); return this; }
        public Builder texto(String cont) { msg.setContenido(cont); return this; }
        public MensajeSocket build() { return msg; }
    }
}