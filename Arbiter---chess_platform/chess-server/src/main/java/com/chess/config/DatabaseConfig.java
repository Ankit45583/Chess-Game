package com.chess.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DatabaseConfig {
    private static DatabaseConfig instance;
    private BlockingQueue<Connection> connectionPool;
    private static final int POOL_SIZE = 10;
    private String url;
    private String username;
    private String password;

    private DatabaseConfig() {
        loadConfig();
        initializeConnectionPool();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("com/chess/config/database.properties")) {
            props.load(fis);
            url = props.getProperty("db.url", "jdbc:mariadb://localhost:3306/chess_db");
            username = props.getProperty("db.username", "root");
            password = props.getProperty("db.password", "root");
        } catch (IOException e) {
            url = "jdbc:mariadb://localhost:3306/chess_db";
            username = "root";
            password = "root";
        }
    }

    private void initializeConnectionPool() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connectionPool = new LinkedBlockingQueue<>(POOL_SIZE);

            for (int i = 0; i < POOL_SIZE; i++) {
                Connection connection = DriverManager.getConnection(url, username, password);
                connectionPool.offer(connection);
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to initialize MariaDB connection pool", e);
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection connection = connectionPool.poll();
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
            }
            return connection;
        } catch (SQLException e) {
            throw new SQLException("Failed to get MariaDB connection", e);
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            connectionPool.offer(connection);
        }
    }

    public void closeAllConnections() {
        for (Connection connection : connectionPool) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}