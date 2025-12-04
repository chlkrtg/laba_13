package ex2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProgressClientFX extends Application {

    private ProgressBar progressBar = new ProgressBar(0);
    private Label statusLabel = new Label("Подключение...");
    private Button startButton = new Button("Старт");
    private Button pauseButton = new Button("Пауза");
    private Button stopButton = new Button("Стоп");

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;
    private final int SERVER_PORT = 12345;
    private final String SERVER_HOST = "localhost";

    @Override
    public void start(Stage primaryStage) {
        progressBar.setPrefWidth(300);

        startButton.setOnAction(e -> sendCommand("START"));
        pauseButton.setOnAction(e -> {
            if ("Пауза".equals(pauseButton.getText())) {
                sendCommand("PAUSE");
            } else {
                sendCommand("RESUME");
            }
        });
        stopButton.setOnAction(e -> sendCommand("STOP"));

        resetControls();

        VBox controlBox = new VBox(5, startButton, pauseButton, stopButton);
        VBox layout = new VBox(15, progressBar, statusLabel, controlBox);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        primaryStage.setTitle("Клиент прогресса");
        primaryStage.setScene(new Scene(layout, 400, 200));
        primaryStage.setOnCloseRequest(e -> disconnect());
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                // подключение к серверу
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true); // ортправка команд серверу
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // получение ответа от сервера

                // ждём подключенияя
                if ("SERVER_CONNECTED".equals(in.readLine())) {
                    connected = true;
                    Platform.runLater(() -> {
                        statusLabel.setText("Подключено к серверу");
                        resetControls(); // разрешаем старт
                    });

                    // запускаем поток для приема сообщений
                    receiveMessages();
                } else {
                    throw new IOException("Неверный ответ от сервера");
                }
            } catch (IOException e) {
                System.err.println("Ошибка подключения: " + e.getMessage());
                Platform.runLater(() -> statusLabel.setText("Не удалось подключиться"));
                disconnect();
            }
        }).start();
    }

    private void receiveMessages() {
        try {
            String message;

            // получение сообщений от сервера
            while (connected && (message = in.readLine()) != null) {
                final String msg = message;
                Platform.runLater(() -> handleServerMessage(msg));
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Соединение потеряно: " + e.getMessage());
            }
        } finally {
            if (connected) { // если не закрывали явно
                Platform.runLater(() -> {
                    statusLabel.setText("Соединение потеряно");
                    resetControls();
                });
            }
            disconnect();
        }
    }

    private void handleServerMessage(String message) {
        System.out.println("Получено: " + message);

        // обработка всевозможных команд
        if (message.startsWith("PROGRESS:")) {
            try {
                double progress = Double.parseDouble(message.substring(9).trim());
                progressBar.setProgress(progress);
                int percent = (int) (progress * 100);
                statusLabel.setText("Выполнено: " + percent + "%");
            } catch (NumberFormatException e) {
                System.err.println("Ошибка формата прогресса: " + message);
            }
        } else if ("TASK_STARTED".equals(message)) {
            statusLabel.setText("Выполняется...");
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
            pauseButton.setText("Пауза");
        } else if ("TASK_PAUSED".equals(message)) {
            statusLabel.setText("Приостановлено");
            pauseButton.setText("Продолжить");
        } else if ("TASK_RESUMED".equals(message)) {
            statusLabel.setText("Выполняется...");
            pauseButton.setText("Пауза");
        } else if ("TASK_STOPPED".equals(message) || "TASK_COMPLETED".equals(message) || "TASK_INTERRUPTED".equals(message)) {
            if ("TASK_COMPLETED".equals(message)) {
                progressBar.setProgress(1.0);
                statusLabel.setText("Завершено!");
            } else if ("TASK_STOPPED".equals(message)) {
                statusLabel.setText("Остановлено");
            } else {
                statusLabel.setText("Прервано");
            }
            progressBar.setProgress(0);
            resetControls();
        } else if (message.startsWith("ERROR:")) {
            System.err.println("Ошибка сервера: " + message.substring(6));
            statusLabel.setText("Ошибка: " + message.substring(6));
        }
    }

    private void sendCommand(String command) {
        if (connected && out != null) {
            out.println(command); // отправляем серверу строку
        } else {
            System.err.println("Нет соединения, команда не отправлена: " + command);
        }
    }

    private void resetControls() {
        startButton.setDisable(!connected); // только если подключён
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        pauseButton.setText("Пауза");
    }

    private void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            if (out != null) {
                sendCommand("DISCONNECT");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Клиент отключен");
        } catch (IOException e) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}