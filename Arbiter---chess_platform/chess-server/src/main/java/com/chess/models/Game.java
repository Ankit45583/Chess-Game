package com.chess.models;

import java.time.LocalDateTime;

public class Game {
    private int gameId;
    private String gameCode;
    private int whitePlayerId;
    private int blackPlayerId;
    private String whitePlayerName;
    private String blackPlayerName;
    private String fenPosition;
    private GameStatus status;
    private GameResult result;
    private String turn;
    private LocalDateTime createdAt;
    private LocalDateTime lastMoveAt;

    public enum GameStatus {
        WAITING, ACTIVE, FINISHED, ABANDONED
    }

    public enum GameResult {
        WHITE_WIN, BLACK_WIN, DRAW, ABANDONED
    }

    public Game() {
        this.fenPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        this.status = GameStatus.WAITING;
        this.turn = "WHITE";
    }

    public Game(String gameCode, int whitePlayerId) {
        this();
        this.gameCode = gameCode;
        this.whitePlayerId = whitePlayerId;
    }

    public int getGameId() { return gameId; }
    public void setGameId(int gameId) { this.gameId = gameId; }

    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }

    public int getWhitePlayerId() { return whitePlayerId; }
    public void setWhitePlayerId(int whitePlayerId) { this.whitePlayerId = whitePlayerId; }

    public int getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(int blackPlayerId) { this.blackPlayerId = blackPlayerId; }

    public String getWhitePlayerName() { return whitePlayerName; }
    public void setWhitePlayerName(String whitePlayerName) { this.whitePlayerName = whitePlayerName; }

    public String getBlackPlayerName() { return blackPlayerName; }
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }

    public String getFenPosition() { return fenPosition; }
    public void setFenPosition(String fenPosition) { this.fenPosition = fenPosition; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }

    public String getTurn() { return turn; }
    public void setTurn(String turn) { this.turn = turn; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMoveAt() { return lastMoveAt; }
    public void setLastMoveAt(LocalDateTime lastMoveAt) { this.lastMoveAt = lastMoveAt; }

    public boolean isWhite(int userId) {
        return userId == whitePlayerId;
    }

    public boolean isBlack(int userId) {
        return userId == blackPlayerId;
    }
}