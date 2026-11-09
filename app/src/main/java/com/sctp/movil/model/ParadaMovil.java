package com.sctp.movil.model;

public class ParadaMovil
{
    private final int id;
    private final int rutaId;
    private final String nombre;
    private final int orden;
    private final int minutosDesdeOrigen;
    private final Double latitud;
    private final Double longitud;

    public ParadaMovil(int id, int rutaId, String nombre, int orden, int minutosDesdeOrigen, Double latitud, Double longitud)
    {
        this.id = id;
        this.rutaId = rutaId;
        this.nombre = nombre;
        this.orden = orden;
        this.minutosDesdeOrigen = minutosDesdeOrigen;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public int getId()
    {
        return id;
    }

    public int getRutaId()
    {
        return rutaId;
    }

    public String getNombre()
    {
        return nombre;
    }

    public int getOrden()
    {
        return orden;
    }

    public int getMinutosDesdeOrigen()
    {
        return minutosDesdeOrigen;
    }

    public Double getLatitud()
    {
        return latitud;
    }

    public Double getLongitud()
    {
        return longitud;
    }
}
