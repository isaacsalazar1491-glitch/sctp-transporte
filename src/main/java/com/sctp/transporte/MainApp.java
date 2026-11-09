package com.sctp.transporte;

import com.sctp.transporte.ui.Navegador;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp
{
    // arranque desacoplado para evitar el error de componentes javafx faltantes
    public static void main(String[] args)
    {
        Application.launch(FxApp.class, args);
    }

    public static class FxApp extends Application
    {
        @Override
        public void start(Stage stage)
        {
            Navegador.iniciar(stage);
            Navegador.irAConsulta();
            stage.setTitle("SCTP · Sistema de Control de Transporte Público");
            stage.setMinWidth(980);
            stage.setMinHeight(640);
            stage.show();
        }
    }
}
