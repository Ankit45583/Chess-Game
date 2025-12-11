package com.chess.http;

import com.chess.service.AuthService;
import com.chess.service.GameService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class ChessHttpServer {
    private final HttpServer server;
    private final AuthService authService;
    private final GameService gameService;
    private final int port;

    public ChessHttpServer(int port, AuthService authService, GameService gameService)
            throws IOException {
        this.port = port;
        this.authService = authService;
        this.gameService = gameService;

        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
        System.out.println("HTTP Server started on port " + port);
    }

    public void stop() {
        server.stop(0);
        System.out.println("HTTP Server stopped");
    }

    private void setupRoutes() {
        server.createContext("/api/auth/register", new AuthHandler());
        server.createContext("/api/auth/login", new AuthHandler());
        server.createContext("/api/game/create", new GameHandler());
        server.createContext("/api/game/join/", new GameHandler());
        server.createContext("/api/game/", new GameHandler());
        server.createContext("/api/user/games", new UserGamesHandler());  // NEW

        server.createContext("/", new StaticFileHandler());
    }

    private class UserGamesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:5173");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String token = getTokenFromHeader(exchange);
                if (token == null || !authService.validateToken(token)) {
                    sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                    return;
                }

                int userId = authService.getUserIdFromToken(token);

                var games = gameService.listGamesForUser(userId); // implement in GameService
                org.json.JSONArray arr = new org.json.JSONArray();
                for (var g : games) {
                    JSONObject obj = new JSONObject();
                    obj.put("gameCode", g.getGameCode());
                    obj.put("status", g.getStatus().toString());
                    obj.put("result", g.getResult() != null ? gameService.getWinnerName(g.getGameCode()) : JSONObject.NULL);
                    obj.put("turn", g.getTurn());
                    obj.put("createdAt", g.getCreatedAt().toString());
                    obj.put("lastMoveAt", g.getLastMoveAt() != null ? g.getLastMoveAt().toString() : JSONObject.NULL);
                    arr.put(obj);
                }

                sendResponse(exchange, 200, arr.toString());
            } catch (Exception e) {
                JSONObject error = new JSONObject();
                error.put("error", e.getMessage());
                sendResponse(exchange, 400, error.toString());
            }
        }
    }


    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:5173");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1); // no body
                return;
            }
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                JSONObject request = parseRequestBody(exchange);
                String path = exchange.getRequestURI().getPath();

                JSONObject response = new JSONObject();

                if (path.endsWith("/register")) {
                    String username = request.getString("username");
                    String password = request.getString("password");

                    String token = authService.register(username, password);
                    response.put("token", token);
                    response.put("message", "Registration successful");

                } else if (path.endsWith("/login")) {
                    String username = request.getString("username");
                    String password = request.getString("password");

                    String token = authService.login(username, password);
                    response.put("token", token);
                    response.put("message", "Login successful");
                }

                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                JSONObject error = new JSONObject();
                error.put("error", e.getMessage());
                sendResponse(exchange, 400, error.toString());
            }
        }
    }

    private class GameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:5173");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String token = getTokenFromHeader(exchange);
                if (token == null || !authService.validateToken(token)) {
                    sendResponse(exchange, 401, "Unauthorized");
                    return;
                }

                int userId = authService.getUserIdFromToken(token);
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");

                JSONObject response = new JSONObject();

                if (path.endsWith("/create") && "POST".equals(exchange.getRequestMethod())) {
                    String gameCode = gameService.createGame(userId);
                    response.put("gameCode", gameCode);
                    response.put("message", "Game created successfully");

                } else if (path.contains("/join/") && "POST".equals(exchange.getRequestMethod())) {
                    String gameCode = parts[parts.length - 1];
                    boolean joined = gameService.joinGame(gameCode, userId);
                    response.put("success", joined);
                    response.put("gameCode", gameCode);

                } else if (path.contains("/game/") && "GET".equals(exchange.getRequestMethod())) {
                    String gameCode = parts[parts.length - 1];
                    if (!gameCode.equals("create") && !gameCode.equals("join")) {
                        JSONObject gameState = gameService.getGameState(gameCode);
                        if (gameState != null) {
                            response = gameState;
                        } else {
                            sendResponse(exchange, 404, "Game not found");
                            return;
                        }
                    }
                }

                sendResponse(exchange, 200, response.toString());

            } catch (Exception e) {
                JSONObject error = new JSONObject();
                error.put("error", e.getMessage());
                sendResponse(exchange, 400, error.toString());
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                path = "/index.html";
            }

            Path filePath = Paths.get("web", path).toAbsolutePath().normalize();

            if (!Files.exists(filePath)) {
                if (path.startsWith("/api/")) {
                    sendResponse(exchange, 404, "Not Found");
                } else {
                    filePath = Paths.get("web", "index.html").toAbsolutePath().normalize();
                }
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            String mimeType = getMimeType(filePath.toString());

            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private String getMimeType(String filename) {
            if (filename.endsWith(".html")) return "text/html";
            if (filename.endsWith(".css")) return "text/css";
            if (filename.endsWith(".js")) return "application/javascript";
            if (filename.endsWith(".json")) return "application/json";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".gif")) return "image/gif";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "application/octet-stream";
        }
    }

    private JSONObject parseRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), "utf-8"))) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            return new JSONObject(requestBody.toString());
        }
    }

    private String getTokenFromHeader(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:5173");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}