package com.chess.dao;

import com.chess.config.DatabaseConfig;
import com.chess.models.Move;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MoveDAO {
    private final DatabaseConfig dbConfig;

    public MoveDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    public void saveMove(Move move) throws SQLException {
        String sql = "INSERT INTO moves (game_id, move_number, from_square, to_square, " +
                "piece, captured_piece, promotion, san_notation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, move.getGameId());
            stmt.setInt(2, move.getMoveNumber());
            stmt.setString(3, move.getFromSquare());
            stmt.setString(4, move.getToSquare());
            stmt.setString(5, move.getPiece());
            stmt.setObject(6, move.getCapturedPiece());
            stmt.setObject(7, move.getPromotion());
            stmt.setString(8, move.getSanNotation());

            stmt.executeUpdate();
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public List<Move> getMovesByGameId(int gameId) throws SQLException {
        String sql = "SELECT * FROM moves WHERE game_id = ? ORDER BY move_number";
        List<Move> moves = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    moves.add(mapResultSetToMove(rs));
                }
            }

            return moves;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public int getNextMoveNumber(int gameId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(move_number), 0) + 1 as next_move FROM moves WHERE game_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("next_move");
                }
            }

            return 1;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    private Move mapResultSetToMove(ResultSet rs) throws SQLException {
        Move move = new Move();
        move.setMoveId(rs.getInt("move_id"));
        move.setGameId(rs.getInt("game_id"));
        move.setMoveNumber(rs.getInt("move_number"));
        move.setFromSquare(rs.getString("from_square"));
        move.setToSquare(rs.getString("to_square"));
        move.setPiece(rs.getString("piece"));
        move.setCapturedPiece(rs.getString("captured_piece"));
        move.setPromotion(rs.getString("promotion"));
        move.setSanNotation(rs.getString("san_notation"));
        move.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return move;
    }
}