package com.sctp.transporte.controller;

import com.sctp.transporte.config.Db;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensajeError;

    @FXML
    public void initialize() {
        if (lblMensajeError != null) {
            lblMensajeError.setText("");
        }
    }

    @FXML
    public void handleLogin() {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            if (lblMensajeError != null) {
                lblMensajeError.setText("Por favor, ingresa usuario y contraseña.");
            }
            return;
        }

        // Validación temporal de acceso por defecto o consulta a base de datos
        if (usuario.equals("admin") && password.equals("admin123")) {
            exitoLogin(usuario);
            return;
        }

        try {
            boolean valido = Db.read(c -> {
                String query = "SELECT * FROM usuarios WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = c.prepareStatement(query)) {
                    stmt.setString(1, usuario);
                    stmt.setString(2, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            });

            if (valido) {
                exitoLogin(usuario);
            } else {
                if (lblMensajeError != null) {
                    lblMensajeError.setText("Usuario o contraseña incorrectos.");
                }
            }
        } catch (Exception e) {
            // Si ocurre algún detalle con la BD, permitimos acceso admin por defecto o mostramos error
            if (usuario.equals("admin") && password.equals("admin123")) {
                exitoLogin(usuario);
            } else {
                if (lblMensajeError != null) {
                    lblMensajeError.setText("Error de conexión al autenticar.");
                }
                e.printStackTrace();
            }
        }
    }

    private void exitoLogin(String usuario) {
        if (mainController != null) {
            mainController.actualizarSesionAdmin(usuario);
        }
        // Cerrar la ventana de login
        Stage stage = (Stage) txtUsuario.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleCancelar() {
        Stage stage = (Stage) txtUsuario.getScene().getWindow();
        stage.close();
    }
}