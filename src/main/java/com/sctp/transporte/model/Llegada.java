package com.sctp.transporte.model;

import java.time.LocalTime;

public record Llegada(Salida salida, LocalTime llegada, int enMinutos) {}
