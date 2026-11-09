package com.sctp.movil.model;

public class RutaMovil
{
    private final int id;
    private final String clave;
    private final String nombre;
    private final String estado;

    public RutaMovil(int id, String clave, String nombre, String estado)
    {
        this.id = id;
        this.clave = clave;
        this.nombre = nombre;
        this.estado = estado;
    }

    public int getId()
    {
        return id;
    }

    public String getClave()
    {
        return clave;
    }

    public String getNombre()
    {
        return nombre;
    }

    public String getEstado()
    {
        return estado;
    }
}

