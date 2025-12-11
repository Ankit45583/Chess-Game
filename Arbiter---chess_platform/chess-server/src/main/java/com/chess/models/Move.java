package com.chess.models;

import java.time.LocalDateTime;

public class Move {
    private int moveId;
    private int gameId;
    private int moveNumber;
    private String fromSquare;
    private String toSquare;
    private String piece;
    private String capturedPiece;
    private String promotion;
    private String sanNotation; // standard algebraic notation
    private LocalDateTime createdAt;

    public int getMoveId() { return moveId; }
    public void setMoveId(int moveId) { this.moveId = moveId; }

    public int getGameId() { return gameId; }
    public void setGameId(int gameId) { this.gameId = gameId; }

    public int getMoveNumber() { return moveNumber; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public String getFromSquare() { return fromSquare; }
    public void setFromSquare(String fromSquare) { this.fromSquare = fromSquare; }

    public String getToSquare() { return toSquare; }
    public void setToSquare(String toSquare) { this.toSquare = toSquare; }

    public String getPiece() { return piece; }
    public void setPiece(String piece) { this.piece = piece; }

    public String getCapturedPiece() { return capturedPiece; }
    public void setCapturedPiece(String capturedPiece) { this.capturedPiece = capturedPiece; }

    public String getPromotion() { return promotion; }
    public void setPromotion(String promotion) { this.promotion = promotion; }

    public String getSanNotation() { return sanNotation; }
    public void setSanNotation(String sanNotation) { this.sanNotation = sanNotation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}