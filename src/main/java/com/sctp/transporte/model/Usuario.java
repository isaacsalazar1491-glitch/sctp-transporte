package com.sctp.transporte.model;

import com.sctp.transporte.model.enums.RolUsuario;

public record Usuario(Integer id, String username, String passwordHash, RolUsuario rol, boolean activo) {}
