package ex3;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 12346;
    private static int matches = 37;
    private static String player1Name = "Игрок 1";
    private static String player2Name = "Игрок 2";
    private static PrintWriter player1Out;
    private static PrintWriter player2Out;
    private static boolean player1Turn = true;
    private static boolean gameOver = false;

    public static void main(String[] args) throws IOException {
        System.out.println("Сервер игры запущен. Ожидание игроков...");

        ServerSocket serverSocket = new ServerSocket(PORT);

        // ждём первого игрока
        Socket player1Socket = serverSocket.accept();
        player1Out = new PrintWriter(player1Socket.getOutputStream(), true);
        new Thread(() -> handlePlayer(player1Socket, true)).start();
        System.out.println("Игрок 1 подключен");

        // ждём второго игрока
        Socket player2Socket = serverSocket.accept();
        player2Out = new PrintWriter(player2Socket.getOutputStream(), true);
        new Thread(() -> handlePlayer(player2Socket, false)).start();
        System.out.println("Игрок 2 подключен");

        // начинаем игру
        broadcast("Игра началась! Спичек: " + matches);
        player1Out.println("YOUR_TURN:Возьмите от 1 до 5 спичек");
        player2Out.println("WAIT:Ходит Игрок 1");
    }

    private static void handlePlayer(Socket socket, boolean isPlayer1) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            String message;
            while ((message = in.readLine()) != null) {
                if (gameOver) continue;

                if (message.startsWith("TAKE:")) {
                    handleMove(isPlayer1, message.substring(5));
                }
            }
        } catch (IOException e) {
            System.out.println("Игрок отключился");
        }
    }

    private static void handleMove(boolean isPlayer1, String moveStr) {
        try {
            int take = Integer.parseInt(moveStr);

            // проверяем правильность хода
            if (take < 1 || take > 5) {
                sendToPlayer(isPlayer1, "ERROR:Можно брать от 1 до 5 спичек");
                return;
            }

            if (take > matches) {
                sendToPlayer(isPlayer1, "ERROR:Осталось только " + matches + " спичек");
                return;
            }

            // проверяем очередь
            if ((isPlayer1 && !player1Turn) || (!isPlayer1 && player1Turn)) {
                sendToPlayer(isPlayer1, "ERROR:Сейчас не ваш ход");
                return;
            }

            // выполняем ход
            matches -= take;
            String playerName = isPlayer1 ? player1Name : player2Name;

            broadcast(playerName + " взял(а) " + take + " спичек. Осталось: " + matches);

            // проверяем конец игры
            if (matches <= 0) {
                gameOver = true;
                player1Out.println(isPlayer1 ? "WIN:Вы победили!" : "LOSE:Вы проиграли!");
                player2Out.println(!isPlayer1 ? "WIN:Вы победили!" : "LOSE:Вы проиграли!");
                System.out.println("Игра окончена. Победитель: " + playerName);
                return;
            }

            // передаем ход другому игроку
            player1Turn = !player1Turn;

            if (player1Turn) {
                player1Out.println("YOUR_TURN:Ваш ход. Осталось: " + matches);
                player2Out.println("WAIT:Ходит Игрок 1");
            } else {
                player2Out.println("YOUR_TURN:Ваш ход. Осталось: " + matches);
                player1Out.println("WAIT:Ходит Игрок 2");
            }

        } catch (NumberFormatException e) {
            sendToPlayer(isPlayer1, "ERROR:Введите число");
        }
    }

    private static void broadcast(String message) {
        System.out.println(message);
        if (player1Out != null) player1Out.println("INFO:" + message);
        if (player2Out != null) player2Out.println("INFO:" + message);
    }

    private static void sendToPlayer(boolean isPlayer1, String message) {
        if (isPlayer1 && player1Out != null) {
            player1Out.println(message);
        } else if (player2Out != null) {
            player2Out.println(message);
        }
    }
}