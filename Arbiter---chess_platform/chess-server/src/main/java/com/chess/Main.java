package com.chess;

import com.chess.config.DatabaseConfig;
import com.chess.dao.GameDAO;
import com.chess.dao.MoveDAO;
import com.chess.dao.UserDAO;
import com.chess.http.ChessHttpServer;
import com.chess.service.AuthService;
import com.chess.service.GameService;
import com.chess.websocket.ChessWebSocketServer;
import org.glassfish.tyrus.server.Server;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Testing MariaDB connection...");
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            System.out.println("i can reach here !!!");

            if (dbConfig.testConnection()) {
                System.out.println("✅ MariaDB connection successful");
            } else {
                System.out.println("Failed to connect to MariaDB");
                System.exit(1);
            }

            UserDAO userDAO = new UserDAO();
            GameDAO gameDAO = new GameDAO();
            MoveDAO moveDAO = new MoveDAO();

            System.out.println("DAOs initialized");

            AuthService authService = new AuthService(userDAO);
            GameService gameService = new GameService(gameDAO, moveDAO, userDAO);

            System.out.println("Services initialized");

            ChessWebSocketServer.setServices(authService, gameService);
            startWebSocketServer();
            startHttpServer(authService, gameService);

            addShutdownHook();

            System.out.println("Chess server is running!");

            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void startHttpServer(AuthService authService, GameService gameService)
            throws IOException {
        int httpPort = 8080;
        ChessHttpServer httpServer = new ChessHttpServer(httpPort, authService, gameService);
        httpServer.start();
        System.out.println("HTTP Server started on port " + httpPort);
    }

    private static void startWebSocketServer() {
        int wsPort = 8081;
        Server server = new Server("localhost", wsPort, "/", null, ChessWebSocketServer.class);

        try {
            server.start();
            System.out.println("WebSocket Server started on port " + wsPort);

        } catch (Exception e) {
            throw new RuntimeException("Failed to start WebSocket server", e);
        }
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down chess server...");
            DatabaseConfig.getInstance().closeAllConnections();
            System.out.println("Database connections closed");
            System.out.println("Chess server stopped");
        }));
    }
}