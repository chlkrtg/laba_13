package ex1;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalculatorServer {
    private static final int PORT = 123;

    public static void main(String[] args) {
        // Создаем пул потоков для обработки клиентов
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 Сервер запущен на порту " + PORT + ". Ожидание подключений...");

            while (true) {
                // Сервер постоянно ждет новых подключений
                Socket clientSocket = serverSocket.accept();
                System.out.println("[ПОДКЛЮЧЕНО] Клиент " + clientSocket.getInetAddress().getHostAddress() + " подключился.");

                // Передаем обработку клиента в отдельный поток из пула
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            pool.shutdown(); // Остановка пула потоков при завершении работы сервера
        }
    }
}

// Отдельный класс для обработки каждого клиента
class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                // Входящий поток для чтения данных от клиента
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // Выходящий поток для отправки данных клиенту
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            String expression;
            // Читаем выражения от клиента, пока не произойдет ошибка или клиент не отключится
            while ((expression = in.readLine()) != null) {
                System.out.println("[ЗАПРОС] От " + clientSocket.getInetAddress().getHostAddress() + ": " + expression);

                String result = calculate(expression);

                out.println(result); // Отправляем результат обратно клиенту
                System.out.println("[ОТВЕТ] Для " + clientSocket.getInetAddress().getHostAddress() + ": " + result);
            }
        } catch (IOException e) {
            // Ошибка, когда клиент внезапно отключился
            System.out.println("[ОТКЛЮЧЕНО] Клиент " + clientSocket.getInetAddress().getHostAddress() + " отключился.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Игнорируем ошибки при закрытии сокета
            }
        }
    }

    private String calculate(String expression) {
        try {
            // Разделение строки на три части: операнд1, оператор, операнд2
            String[] parts = expression.trim().split("\\s+");

            if (parts.length != 3) {
                return "Ошибка: Неверный формат выражения. Используйте: число1 операция число2";
            }

            double operand1 = Double.parseDouble(parts[0]);
            String operator = parts[1];
            double operand2 = Double.parseDouble(parts[2]);

            double result = 0;

            // Последовательное выполнение операций
            switch (operator) {
                case "+":
                    result = operand1 + operand2;
                    break;
                case "-":
                    result = operand1 - operand2;
                    break;
                case "*":
                    result = operand1 * operand2;
                    break;
                case "/":
                    if (operand2 == 0) {
                        return "Ошибка: Деление на ноль!";
                    }
                    result = operand1 / operand2;
                    break;
                default:
                    return "Ошибка: Неизвестная операция. Доступны: +, -, *, /";
            }

            // Возвращаем результат как строку
            return String.valueOf(result);

        } catch (NumberFormatException e) {
            return "Ошибка: Операнды должны быть вещественными числами.";
        } catch (Exception e) {
            return "Неизвестная ошибка вычисления.";
        }
    }
}