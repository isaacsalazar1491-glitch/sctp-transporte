package com.sctp.transporte.controller;

import com.sctp.transporte.service.AuthService;
import com.sctp.transporte.ui.Fmt;
import com.sctp.transporte.ui.Navegador;
import com.sctp.transporte.ui.Sesion;
import com.sctp.transporte.ui.Tareas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Acceso del administrador (AuthService, BCrypt). */
public class LoginController {
    @FXML private TextField usuario;
    @FXML private PasswordField clave;
    @FXML private Label error;
    @FXML private Button entrar;

    private final AuthService auth = new AuthService();

    @FXML
    private void initialize() {
        Fmt.banner(error, null, true);
        Platform.runLater(usuario::requestFocus);
    }

    @FXML
    private void iniciarSesion() {
        String u = usuario.getText();
        String p = clave.getText();
        Fmt.banner(error, null, true);
        entrar.setDisable(true);
        Tareas.ejecutar(() -> auth.autenticar(u, p),
                user -> {
                    Sesion.iniciar(user);
                    Navegador.irAAdmin();
                },
                msg -> {
                    entrar.setDisable(false);
                    clave.clear();
                    Fmt.banner(error, msg, true);
                });
    }

    @FXML
    private void volver() { Navegador.irAConsulta(); }
}
