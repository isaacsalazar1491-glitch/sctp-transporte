package com.sctp.movil.model;

public class IncidenciaMovil
{
    private final int id;
    private final String tipo;
    private final String descripcion;
    private final String severidad;
    private final String fecha;
    private final String estado;

    public IncidenciaMovil(int id, String tipo, String descripcion, String severidad, String fecha)
    {
        this(id, tipo, descripcion, severidad, fecha, "ABIERTA");
    }

    public IncidenciaMovil(int id, String tipo, String descripcion, String severidad, String fecha, String estado)
    {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.fecha = fecha;
        this.estado = estado;
    }

    public int getId()
    {
        return id;
    }

    public String getTipo()
    {
        return tipo;
    }

    public String getDescripcion()
    {
        return descripcion;
    }

    public String getSeveridad()
    {
        return severidad;
    }

    public String getFecha()
    {
        return fecha;
    }

    public String getEstado()
    {
        return estado;
    }
}
