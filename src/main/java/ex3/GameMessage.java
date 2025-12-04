package ex3;

import java.io.Serializable;

public class GameMessage implements Serializable {
    private int remainingMatches;
    private int currentPlayerId;
    private String statusMessage;
    private boolean isGameOver;

    public GameMessage(int remainingMatches, int currentPlayerId, String statusMessage, boolean isGameOver) {
        this.remainingMatches = remainingMatches;
        this.currentPlayerId = currentPlayerId;
        this.statusMessage = statusMessage;
        this.isGameOver = isGameOver;
    }

    public int getRemainingMatches() { return remainingMatches; }
    public int getCurrentPlayerId() { return currentPlayerId; }
    public String getStatusMessage() { return statusMessage; }
    public boolean isGameOver() { return isGameOver; }
}