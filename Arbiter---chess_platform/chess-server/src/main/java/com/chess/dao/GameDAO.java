package com.chess.dao;

import com.chess.config.DatabaseConfig;
import com.chess.models.Game;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameDAO {
    private final DatabaseConfig dbConfig;

    public GameDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    public Game createGame(Game game) throws SQLException {
        String sql = "INSERT INTO games (game_code, white_player_id, black_player_id, fen_position, " +
                "status, turn) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, game.getGameCode());
            stmt.setInt(2, game.getWhitePlayerId());
            stmt.setObject(3, game.getBlackPlayerId() > 0 ? game.getBlackPlayerId() : null);
            stmt.setString(4, game.getFenPosition());
            stmt.setString(5, game.getStatus().toString());
            stmt.setString(6, game.getTurn());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating game failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    game.setGameId(generatedKeys.getInt(1));
                }
            }

            return game;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public Game findByCode(String gameCode) throws SQLException {
        String sql = "SELECT g.*, u1.username as white_username, u2.username as black_username " +
                "FROM games g " +
                "LEFT JOIN users u1 ON g.white_player_id = u1.user_id " +
                "LEFT JOIN users u2 ON g.black_player_id = u2.user_id " +
                "WHERE g.game_code = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gameCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGame(rs);
                }
            }

            return null;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }
    public List<Game> findByUser(int userId) throws SQLException {
        String sql = "SELECT g.* FROM games g WHERE g.white_player_id = ? OR g.black_player_id = ?"
                + " ORDER BY g.created_at DESC LIMIT 50";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Game> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapResultSetToGame(rs));
                }
                return result;
            }
        }
    }


    public void updateGame(Game game) throws SQLException {
        String sql = "UPDATE games SET black_player_id = ?, fen_position = ?, status = ?, " +
                "result = ?, turn = ?, last_move_at = ? WHERE game_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, game.getBlackPlayerId() > 0 ? game.getBlackPlayerId() : null);
            stmt.setString(2, game.getFenPosition());
            stmt.setString(3, game.getStatus().toString());
            stmt.setObject(4, game.getResult() != null ? game.getResult().toString() : null);
            stmt.setString(5, game.getTurn());
            stmt.setTimestamp(6, game.getLastMoveAt() != null ?
                    Timestamp.valueOf(game.getLastMoveAt()) : null);
            stmt.setInt(7, game.getGameId());

            stmt.executeUpdate();
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public boolean gameCodeExists(String gameCode) throws SQLException {
        String sql = "SELECT 1 FROM games WHERE game_code = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gameCode);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public List<Game> findWaitingGames() throws SQLException {
        String sql = "SELECT g.*, u1.username as white_username " +
                "FROM games g " +
                "JOIN users u1 ON g.white_player_id = u1.user_id " +
                "WHERE g.status = 'WAITING' AND g.black_player_id IS NULL " +
                "ORDER BY g.created_at DESC";

        List<Game> games = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                games.add(mapResultSetToGame(rs));
            }

            return games;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    private Game mapResultSetToGame(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setGameId(rs.getInt("game_id"));
        game.setGameCode(rs.getString("game_code"));
        game.setWhitePlayerId(rs.getInt("white_player_id"));
        game.setBlackPlayerId(rs.getInt("black_player_id"));
        game.setFenPosition(rs.getString("fen_position"));
        game.setStatus(Game.GameStatus.valueOf(rs.getString("status")));

        String result = rs.getString("result");
        if (result != null) {
            game.setResult(Game.GameResult.valueOf(result));
        }

        game.setTurn(rs.getString("turn"));
        game.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp lastMove = rs.getTimestamp("last_move_at");
        if (lastMove != null) {
            game.setLastMoveAt(lastMove.toLocalDateTime());
        }

        try {
            game.setWhitePlayerName(rs.getString("white_username"));
            game.setBlackPlayerName(rs.getString("black_username"));
        } catch (SQLException e) {
        }

        return game;
    }
}