package ex2;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ProgressServer {
    private static final int PORT = 12345;
    private static ExecutorService clientThreadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер прогресса запущен на порту " + PORT);

            while (true) { // принимаем клиентов бесконечно
                Socket clientSocket = serverSocket.accept();
                System.out.println("Новое подключение: " + clientSocket.getInetAddress());

                // запускаем обработчик клиента
                clientThreadPool.execute(new ProgressHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            clientThreadPool.shutdown();
        }
    }
}

class ProgressHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private volatile boolean taskRunning = false;
    private volatile boolean taskPaused = false;
    private Thread taskThread;

    public ProgressHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                InputStreamReader isr = new InputStreamReader(clientSocket.getInputStream()); // получение потока байтов и их преобразование
                BufferedReader clientIn = new BufferedReader(isr); // буфферизация входных данных
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true); // байтовый поток для отправки клиенту
        ) {
            this.in = clientIn;
            this.out = clientOut;

            // отправляет подтверждение подключения
            out.println("SERVER_CONNECTED");

            // чтение команд от клиента
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Получена команда: " + command);

                switch (command) {
                    case "START":
                        startTask();
                        break;
                    case "PAUSE":
                        pauseTask();
                        break;
                    case "RESUME":
                        resumeTask();
                        break;
                    case "STOP":
                        stopTask(false);
                        break;
                    case "DISCONNECT":
                        disconnectClient();
                        return;
                    default:
                        out.println("ERROR: Неизвестная команда");
                }
            }
        } catch (IOException e) {
            System.err.println("Клиент отключился или ошибка связи: " + e.getMessage());
        } finally {
            stopTask(true);
            try {
                if (!clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) { /* Игнорируем */ }
            System.out.println("Обработка клиента завершена.");
        }
    }

    private synchronized void startTask() {
        if (taskRunning) {
            out.println("ERROR: Задача уже выполняется");
            return;
        }

        taskRunning = true;
        taskPaused = false;
        out.println("TASK_STARTED");

        taskThread = new Thread(this::runTask);
        taskThread.start();
    }

    private synchronized void pauseTask() {
        if (!taskRunning || taskPaused) {
            return;
        }
        taskPaused = true;
        out.println("TASK_PAUSED");
    }

    private synchronized void resumeTask() {
        if (!taskRunning || !taskPaused) {
            return;
        }
        taskPaused = false;
        notifyAll();
        out.println("TASK_RESUMED");
    }

    private synchronized void stopTask(boolean silent) {
        if (!taskRunning) {
            return;
        }

        taskRunning = false;
        taskPaused = false;

        notifyAll(); // разбудить поток, если он ждет паузы
        if (taskThread != null) {
            taskThread.interrupt(); // прервать цикл
        }

        if (!silent) {
            out.println("TASK_STOPPED");
        }
    }

    private void disconnectClient() {
        stopTask(true);
        out.println("DISCONNECTED");
    }

    private void runTask() {
        final int TOTAL_ITERATIONS = 1000;

        try {
            for (int i = 0; i <= TOTAL_ITERATIONS; i++) {
                // если задача на паузе - ждёмм
                synchronized (this) {
                    while (taskPaused && !Thread.interrupted()) {
                        wait();
                    }
                }

                if (Thread.interrupted()) {
                    break;
                }

                Thread.sleep(20);

                // Отправка прогресса
                double progress = (double) i / TOTAL_ITERATIONS;
                out.println("PROGRESS:" + String.valueOf(progress));
            }

            // если вышли из цикла не по прерыванию
            if (!Thread.interrupted()) {
                out.println("TASK_COMPLETED");
            } else {
                out.println("TASK_INTERRUPTED");
            }
        } catch (InterruptedException e) {
            out.println("TASK_INTERRUPTED");
        } finally {
            taskRunning = false;
            taskPaused = false;
        }
    }
}