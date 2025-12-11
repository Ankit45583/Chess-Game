package com.chess.websocket;

import com.chess.service.AuthService;
import com.chess.service.GameService;
import com.chess.models.Game;
import org.json.JSONObject;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint("/com/chess/{gameCode}")
public class ChessWebSocketServer {
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionToGame = new ConcurrentHashMap<>();
    private static final Map<String, Integer> sessionToUser = new ConcurrentHashMap<>();
    private static final AtomicInteger sessionIdCounter = new AtomicInteger(1);

    private static AuthService authService;
    private static GameService gameService;

    public static void setServices(AuthService authService, GameService gameService) {
        ChessWebSocketServer.authService = authService;
        ChessWebSocketServer.gameService = gameService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("gameCode") String gameCode) {
        try {
            String token = getTokenFromQuery(session.getQueryString());

            if (token == null || !authService.validateToken(token)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                        "Invalid token"));
                return;
            }

            int userId = authService.getUserIdFromToken(token);

            String sessionId = "session-" + sessionIdCounter.getAndIncrement();
            sessions.put(sessionId, session);
            sessionToGame.put(sessionId, gameCode);
            sessionToUser.put(sessionId, userId);

            session.getUserProperties().put("sessionId", sessionId);

            sendMessage(session, createMessage("CONNECTED",
                    "Connected to game " + gameCode));

            broadcastToGame(gameCode, sessionId, createMessage("PLAYER_JOINED",
                    "Player joined the game"));

            broadcastToGame(gameCode, sessionId, createMessage("PLAYER_JOINED",
                    "Player joined the game"));

            sendFullGameStateToAll(gameCode);

        } catch (Exception e) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                        "Connection failed"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            String sessionId = (String) session.getUserProperties().get("sessionId");
            String gameCode = sessionToGame.get(sessionId);
            int userId = sessionToUser.get(sessionId);

            JSONObject jsonMessage = new JSONObject(message);
            String type = jsonMessage.getString("type");

            switch (type) {
                case "MOVE":
                    handleMove(gameCode, userId, jsonMessage, session);
                    break;
                case "RESIGN":
                    handleResign(gameCode, userId, session);
                    break;
                default:
                    sendMessage(session, createMessage("ERROR", "Unknown message type"));
            }

        } catch (Exception e) {
            sendMessage(session, createMessage("ERROR", e.getMessage()));
        }
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = (String) session.getUserProperties().get("sessionId");

        if (sessionId != null) {
            String gameCode = sessionToGame.get(sessionId);

            sessions.remove(sessionId);
            sessionToGame.remove(sessionId);
            sessionToUser.remove(sessionId);

            if (gameCode != null) {
                broadcastToGame(gameCode, sessionId,
                        createMessage("PLAYER_LEFT", "Player disconnected"));
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
        throwable.printStackTrace();
    }

    private void handleMove(String gameCode, int userId, JSONObject message, Session session) {
        try {
            String from = message.getString("from").toUpperCase();
            String to = message.getString("to").toUpperCase();
            String promotion = message.optString("promotion", null);

            Game game = gameService.makeMove(gameCode, userId, from, to, promotion);

            JSONObject gameState = gameService.getGameState(gameCode);
            if (gameState == null) {
                throw new IllegalStateException("Game not found for code " + gameCode);
            }

            gameState.put("lastMove", new JSONObject()
                    .put("from", from)
                    .put("to", to));

            broadcastGameUpdate(gameCode, game, gameState);

            if (game.getStatus() == Game.GameStatus.FINISHED) {
                JSONObject result = new JSONObject();
                result.put("winner", gameService.getWinnerName(gameCode));
                result.put("reason", "Game finished");
                result.put("loser", gameService.getLoserName(gameCode));

                broadcastToGame(gameCode, null, createMessage("GAME_END", result));
            }

        } catch (Exception e) {
            sendMessage(session, createMessage("MOVE_INVALID", e.getMessage()));
        }
    }

    private void broadcastGameUpdate(String gameCode, Game game, JSONObject gameState) {
        for (Map.Entry<String, String> entry : sessionToGame.entrySet()) {
            if (!entry.getValue().equals(gameCode)) {
                continue;
            }

            String sessionId = entry.getKey();
            Session targetSession = sessions.get(sessionId);
            if (targetSession == null || !targetSession.isOpen()) {
                continue;
            }

            Integer userId = sessionToUser.get(sessionId);
            if (userId == null) {
                continue;
            }

            String yourSide = getUserSide(game, userId);

            JSONObject message = new JSONObject();
            message.put("type", "GAME_UPDATE");
            message.put("data", gameState);
            message.put("yourSide", yourSide);
            message.put("timestamp", System.currentTimeMillis());

            sendMessage(targetSession, message);
        }
    }

    private String getUserSide(Game game, int userId) {
        if (game.getWhitePlayerId() == userId) {
            return "WHITE";
        } else if (game.getBlackPlayerId() == userId) {
            return "BLACK";
        }
        return "SPECTATOR";
    }

    private void sendFullGameStateToAll(String gameCode) {
        try {
            JSONObject gameState = gameService.getGameState(gameCode);
            if (gameState == null) {
                return;
            }

            Game game = gameService.getGame(gameCode);
            if (game == null) {
                return;
            }

            broadcastGameUpdate(gameCode, game, gameState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResign(String gameCode, int userId, Session session) {
        try {
            gameService.resignGame(gameCode, userId);

            JSONObject result = new JSONObject();
            result.put("winner", gameService.getWinnerName(gameCode));
            result.put("reason", "Resigned");
            result.put("loser", gameService.getLoserName(gameCode));

            broadcastToGame(gameCode, null, createMessage("GAME_END", result));

        } catch (Exception e) {
            sendMessage(session, createMessage("ERROR", e.getMessage()));
        }
    }

    private void broadcastToGame(String gameCode, String excludeSessionId, JSONObject message) {
        for (Map.Entry<String, String> entry : sessionToGame.entrySet()) {
            if (entry.getValue().equals(gameCode) &&
                    !entry.getKey().equals(excludeSessionId)) {

                Session targetSession = sessions.get(entry.getKey());
                if (targetSession != null && targetSession.isOpen()) {
                    sendMessage(targetSession, message);
                }
            }
        }
    }

    private void sendMessage(Session session, JSONObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createMessage(String type, Object data) {
        JSONObject message = new JSONObject();
        message.put("type", type);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }

    private String getTokenFromQuery(String queryString) {
        if (queryString == null)
            return null;

        String[] params = queryString.split("&");
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
