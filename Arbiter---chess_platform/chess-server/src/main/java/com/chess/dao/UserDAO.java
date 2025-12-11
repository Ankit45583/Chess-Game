package com.chess.dao;

import com.chess.config.DatabaseConfig;
import com.chess.models.User;
import java.sql.*;

public class UserDAO {
    private final DatabaseConfig dbConfig;

    public UserDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, rating, wins, losses, draws) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setInt(3, user.getRating());
            stmt.setInt(4, user.getWins());
            stmt.setInt(5, user.getLosses());
            stmt.setInt(6, user.getDraws());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            return user;
        } finally {
            dbConfig.releaseConnection(null); // Connection already closed by try-with-resources
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

            return null;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

            return null;
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public void updateStats(int userId, String result) throws SQLException {
        String sql;
        switch (result.toUpperCase()) {
            case "WIN":
                sql = "UPDATE users SET wins = wins + 1, rating = rating + 10 WHERE user_id = ?";
                break;
            case "LOSS":
                sql = "UPDATE users SET losses = losses + 1, rating = rating - 10 WHERE user_id = ?";
                break;
            case "DRAW":
                sql = "UPDATE users SET draws = draws + 1 WHERE user_id = ?";
                break;
            default:
                return;
        }

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } finally {
            dbConfig.releaseConnection(null);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRating(rs.getInt("rating"));
        user.setWins(rs.getInt("wins"));
        user.setLosses(rs.getInt("losses"));
        user.setDraws(rs.getInt("draws"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }
}