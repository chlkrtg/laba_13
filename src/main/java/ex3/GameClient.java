package ex3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;

public class GameClient extends Application {
    private Label statusLabel = new Label("Подключение к серверу...");
    private Label matchesLabel = new Label("Спичек: 37");
    private TextField inputField = new TextField();
    private Button takeButton = new Button("Взять");
    private TextArea logArea = new TextArea();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean myTurn = false;
    private String playerName;

    @Override
    public void start(Stage stage) {
        // получаем имя игрока из параметров
        playerName = getParameters().getUnnamed().size() > 0 ?
                getParameters().getUnnamed().get(0) : "Игрок";

        stage.setTitle(playerName + " - Игра в спички");

        // Настройка интерфейса
        inputField.setPromptText("Сколько спичек взять (1-5)");
        takeButton.setDisable(true);
        logArea.setEditable(false);
        logArea.setPrefHeight(150);

        // обработчик кнопки
        takeButton.setOnAction(e -> {
            String text = inputField.getText();
            if (!text.isEmpty()) {
                out.println("TAKE:" + text);
                inputField.clear();
            }
        });

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 20;");
        root.getChildren().addAll(
                statusLabel,
                matchesLabel,
                new Label("Ваш ход:"),
                inputField,
                takeButton,
                new Label("Лог игры:"),
                logArea
        );

        stage.setScene(new Scene(root, 400, 400));
        stage.show();

        // подключаемся к серверу
        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 12346);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Platform.runLater(() ->
                        statusLabel.setText(playerName + " подключен"));

                // слушаем сервер
                listenServer();

            } catch (IOException e) {
                Platform.runLater(() ->
                        statusLabel.setText("Ошибка подключения: " + e.getMessage()));
            }
        }).start();
    }

    private void listenServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> handleMessage(finalMessage));
            }
        } catch (IOException e) {
            Platform.runLater(() ->
                    statusLabel.setText("Соединение с сервером потеряно"));
        }
    }

    private void handleMessage(String message) {
        // добавляем в лог
        logArea.appendText(message + "\n");

        // обрабатываем команды
        if (message.startsWith("INFO:")) {
            // информационное сообщение
            String info = message.substring(5);
            if (info.contains("Осталось:")) {
                String count = info.split(":")[1].trim();
                matchesLabel.setText("Спичек: " + count);
            }
        }
        else if (message.startsWith("YOUR_TURN:")) {
            // ваш ход
            myTurn = true;
            takeButton.setDisable(false);
            statusLabel.setText(message.substring(10));
        }
        else if (message.startsWith("WAIT:")) {
            // ход другого игрока
            myTurn = false;
            takeButton.setDisable(true);
            statusLabel.setText(message.substring(5));
        }
        else if (message.startsWith("WIN:")) {
            // победа!
            myTurn = false;
            takeButton.setDisable(true);
            statusLabel.setText("🎉 " + message.substring(4));
            showAlert("Поздравляем!", "Вы победили!");
        }
        else if (message.startsWith("LOSE:")) {
            // поражение
            myTurn = false;
            takeButton.setDisable(true);
            statusLabel.setText("😔 " + message.substring(5));
            showAlert("Игра окончена", "Вы проиграли");
        }
        else if (message.startsWith("ERROR:")) {
            // ошибка
            statusLabel.setText("⚠ " + message.substring(6));
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}