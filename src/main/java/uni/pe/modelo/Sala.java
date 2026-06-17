package uni.pe.modelo;

public class Sala {
    private int idSala;
    private String codigoSala;
    private String nombre;
    private int idHost;
    private String estado;
    private String fechaCreacion;

    public Sala() {}

    public Sala(String codigoSala, String nombre, int idHost) {
        this.codigoSala = codigoSala;
        this.nombre = nombre;
        this.idHost = idHost;
        this.estado = "Activa";
    }

    public int getIdSala() { return idSala; }
    public void setIdSala(int idSala) { this.idSala = idSala; }
    public String getCodigoSala() { return codigoSala; }
    public void setCodigoSala(String codigoSala) { this.codigoSala = codigoSala; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getIdHost() { return idHost; }
    public void setIdHost(int idHost) { this.idHost = idHost; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}