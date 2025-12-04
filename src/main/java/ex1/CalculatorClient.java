package ex1;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class CalculatorClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 123;

    public static void main(String[] args) {
        try (
                // Создаем сокет и подключаемся к серверу
                Socket socket = new Socket(HOST, PORT);

                // Выходящий поток для отправки данных на сервер
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // Входящий поток для чтения данных с сервера
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Scanner для чтения пользовательского ввода с консоли
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("✅ Подключено к серверу " + HOST + ":" + PORT + ".");
            System.out.println("Введите выражение в формате: <число1> <операция> <число2> (например, 5.5 + -2.5). Для выхода введите 'exit'.");

            String userInput;
            while (true) {
                System.out.print(">>> Калькулятор: ");
                userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }

                // Отправляем выражение на сервер
                out.println(userInput);

                // Получаем и выводим ответ от сервера
                String response = in.readLine();
                System.out.println("   [Результат] = " + response);
            }

        } catch (ConnectException e) {
            System.err.println("❌ Ошибка: Не удалось подключиться к серверу. Убедитесь, что сервер запущен на " + HOST + ":" + PORT + ".");
        } catch (IOException e) {
            System.err.println("❌ Ошибка ввода/вывода: " + e.getMessage());
        } finally {
            System.out.println("👋 Клиент завершил работу.");
        }
    }
}