package uni.pe.protocolo;

public class MensajeSocket {
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST", LOGIN_RESPONSE = "LOGIN_RESPONSE",
            REGISTER_REQUEST = "REGISTER_REQUEST", REGISTER_RESPONSE = "REGISTER_RESPONSE",
            CREATE_ROOM = "CREATE_ROOM", CREATE_ROOM_RESPONSE = "CREATE_ROOM_RESPONSE",
            JOIN_ROOM_REQUEST = "JOIN_ROOM_REQUEST", WAITING_ROOM_UPDATE = "WAITING_ROOM_UPDATE",
            ADMIT_USER = "ADMIT_USER", ADMIT_RESPONSE = "ADMIT_RESPONSE", CHAT_MESSAGE = "CHAT_MESSAGE",
            FILE_START = "FILE_START", FILE_CHUNK = "FILE_CHUNK", FILE_END = "FILE_END",
            FILE_NOTIFY = "FILE_NOTIFY", FILE_DOWNLOAD_REQUEST = "FILE_DOWNLOAD_REQUEST",
            CAMERA_FRAME = "CAMERA_FRAME", AUDIO_FRAME = "AUDIO_FRAME",
            SCREEN_SHARE = "SCREEN_SHARE", SCREEN_SHARE_STOP = "SCREEN_SHARE_STOP",
            LEAVE_ROOM = "LEAVE_ROOM", ROOM_CLOSED = "ROOM_CLOSED", ERROR = "ERROR";

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
    public void setChunkIndex(int i) { chunkIndex = i; } public int getChunkIndex() { return chunkIndex; }
    public void setTotalChunks(int t) { totalChunks = t; } public int getTotalChunks() { return totalChunks; }
    public void setTamanio(long t) { tamanio = t; } public long getTamanio() { return tamanio; }
    public void setFrameBase64(String f) { frameBase64 = f; }
    public void setAudioBase64(String a) { audioBase64 = a; } public void setAceptado(boolean a) { aceptado = a; }
    public boolean isAceptado() { return aceptado; }
    public String getFrameBase64() { return frameBase64; }
    public String getAudioBase64() { return audioBase64; }

    public static class Builder {
        private MensajeSocket msg;

        public Builder(String type) {
            if (type == null || type.isBlank()) throw new IllegalArgumentException("type requerido");
            msg = new MensajeSocket();
            msg.setType(type);
        }

        private Builder check() {
            if (msg == null) throw new IllegalStateException("Builder ya fue consumido por build()");
            return this;
        }

        public Builder usuario(int id, String nombre)           { check(); msg.setIdUsuario(id); msg.setNombreUsuario(nombre); return this; }
        public Builder sala(String code)                        { check(); msg.setRoomCode(code); return this; }
        public Builder sala(String code, String name)           { check(); msg.setRoomCode(code); msg.setRoomName(name); return this; }
        public Builder respuesta(boolean exito, String mensaje) { check(); msg.setExito(exito); msg.setMensaje(mensaje); return this; }
        public Builder texto(String cont)                       { check(); msg.setContenido(cont); return this; }
        public Builder frame(String base64)                     { check(); msg.setFrameBase64(base64); return this; }
        public Builder audio(String base64)                     { check(); msg.setAudioBase64(base64); return this; }

        public Builder archivoInfo(String nombreArchivo, long tamanio) {
            check();
            msg.setNombreArchivo(nombreArchivo);
            msg.setTamanio(tamanio);
            return this;
        }

        public Builder archivoChunk(String nombreArchivo, String chunkBase64, int chunkIndex, int totalChunks) {
            check();
            msg.setNombreArchivo(nombreArchivo);
            msg.setChunkBase64(chunkBase64);
            msg.setChunkIndex(chunkIndex);
            msg.setTotalChunks(totalChunks);
            return this;
        }

        public Builder credenciales(String correo, String password, String nombres) {
            check();
            msg.setCorreo(correo);
            msg.setPassword(password);
            if (nombres != null) msg.setNombres(nombres);
            return this;
        }

        public MensajeSocket build() {
            check();
            MensajeSocket resultado = msg;
            msg = null; // invalida el builder para detectar reusos accidentales
            return resultado;
        }
    }
}