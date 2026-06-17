package uni.pe.modelo;

public class Mensaje {
    private int idMensaje;
    private int idSala;
    private int idUsuario;
    private String nombreUsuario;
    private String contenido;
    private String enviadoEn;

    public Mensaje() {}

    public Mensaje(int idSala, int idUsuario, String nombreUsuario, String contenido) {
        this.idSala = idSala;
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.contenido = contenido;
    }

    public int getIdMensaje() { return idMensaje; }
    public void setIdMensaje(int idMensaje) { this.idMensaje = idMensaje; }
    public int getIdSala() { return idSala; }
    public void setIdSala(int idSala) { this.idSala = idSala; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getEnviadoEn() { return enviadoEn; }
    public void setEnviadoEn(String enviadoEn) { this.enviadoEn = enviadoEn; }
}