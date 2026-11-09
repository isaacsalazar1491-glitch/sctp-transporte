package com.sctp.transporte.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SeguridadController {

    private int segundosRestantes = 10;
    private Timeline timeline;

    @FXML
    public void initialize() {
        // Inicialización
    }

    private void copiarTexto(String texto) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(texto);
        clipboard.setContent(content);
        System.out.println("Copiado al portapapeles: " + texto);
    }

    @FXML public void copiar911() { copiarTexto("911"); }
    @FXML public void copiarCruzRoja() { copiarTexto("065"); }
    @FXML public void copiarPolicia() { copiarTexto("089"); }

    @FXML public void llamar911() { mostrarAlertaLlamada("911 (Emergencias)"); }
    @FXML public void llamarCruzRoja() { mostrarAlertaLlamada("065 (Cruz Roja)"); }
    @FXML public void llamarPolicia() { mostrarAlertaLlamada("089 (Policía / Denuncia)"); }

    private void mostrarAlertaLlamada(String numero) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Marcador Telefónico");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #0f172a;");

        Label lbl = new Label("📞 Conectando con: " + numero);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label sub = new Label("(Simulación de llamada de escritorio)");
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> stage.close());

        root.getChildren().addAll(lbl, sub, btnCerrar);
        stage.setScene(new Scene(root, 320, 180));
        stage.showAndWait();
    }

    @FXML
    public void activarBotonEmergencia() {
        segundosRestantes = 10; // Reiniciar a 10 segundos

        Stage stageEmergencia = new Stage();
        stageEmergencia.initModality(Modality.APPLICATION_MODAL);
        stageEmergencia.setTitle("🚨 Alerta de Emergencia SCTP");

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #111827; -fx-padding: 40;");

        Label titulo = new Label("Activando protocolo\nde emergencia");
        titulo.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-alignment: center;");

        Label subtitulo = new Label("¿Tocaste el botón de emergencia por error?\nTienes 10 segundos para cancelar.");
        subtitulo.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-text-alignment: center;");

        Label lblContador = new Label(String.valueOf(segundosRestantes));
        lblContador.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 72px; -fx-font-weight: bold;");

        Button btnCancelar = new Button("Cancelar Alerta");
        btnCancelar.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");

        btnCancelar.setOnAction(e -> {
            if (timeline != null) timeline.stop();
            stageEmergencia.close();
        });

        root.getChildren().addAll(titulo, subtitulo, lblContador, btnCancelar);
        Scene scene = new Scene(root, 400, 450);
        stageEmergencia.setScene(scene);

        // Configurar la cuenta regresiva de 10 a 0 segundos usando KeyFrame correctamente
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            segundosRestantes--;
            lblContador.setText(String.valueOf(segundosRestantes));
            if (segundosRestantes <= 0) {
                timeline.stop();
                titulo.setText("¡Alerta Transmitida!");
                subtitulo.setText("Se han notificado las unidades de apoyo cercanas.");
                lblContador.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 24px; -fx-font-weight: bold;");
                lblContador.setText("✔ EMERGENCIA ENVIADA");
                btnCancelar.setText("Cerrar ventana");
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        stageEmergencia.showAndWait();
    }
}