package com.sctp.transporte.model;

import java.time.LocalDate;

/** diasRestantes negativo = licencia ya vencida. */
public record LicenciaPorVencer(String conductor, LocalDate vence, int diasRestantes) {}
