package com.sctp.transporte;

import com.sctp.transporte.ui.Navegador;
import javafx.application.Application;
import javafx.stage.Stage;

/** Punto de entrada de la aplicación JavaFX. Arranca en la consulta pública (sin sesión). */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Navegador.iniciar(stage);
        Navegador.irAConsulta();
        stage.setTitle("SCTP · Sistema de Control de Transporte Público");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
