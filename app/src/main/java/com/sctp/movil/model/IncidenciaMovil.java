package com.sctp.movil.model;

public class IncidenciaMovil
{
    private final int id;
    private final String tipo;
    private final String descripcion;
    private final String severidad;
    private final String fecha;

    public IncidenciaMovil(int id, String tipo, String descripcion, String severidad, String fecha)
    {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.severidad = severidad;
        this.fecha = fecha;
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
}

