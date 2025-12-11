package com.chess.service;

import com.chess.dao.GameDAO;
import com.chess.dao.MoveDAO;
import com.chess.dao.UserDAO;
import com.chess.models.Game;
import com.chess.models.Move;
import com.github.bhlangonijr.chesslib.*;
import org.json.JSONObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class GameService {
    private final GameDAO gameDAO;
    private final MoveDAO moveDAO;
    private final UserDAO userDAO;
    private final Random random = new Random();

    public GameService(GameDAO gameDAO, MoveDAO moveDAO, UserDAO userDAO) {
        this.gameDAO = gameDAO;
        this.moveDAO = moveDAO;
        this.userDAO = userDAO;
    }

    public String createGame(int userId) throws SQLException {
        String gameCode;

        do {
            gameCode = String.format("%06d", random.nextInt(1000000));
        } while (gameDAO.gameCodeExists(gameCode));

        Game game = new Game(gameCode, userId);
        gameDAO.createGame(game);

        return gameCode;
    }

    public Game getGame(String gameCode) throws SQLException {
        return gameDAO.findByCode(gameCode);
    }

    public boolean joinGame(String gameCode, int userId) throws SQLException {
        Game game = gameDAO.findByCode(gameCode);

        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }

        if (game.getStatus() != Game.GameStatus.WAITING) {
            throw new IllegalArgumentException("Game is not waiting for players");
        }

        if (game.getWhitePlayerId() == userId) {
            throw new IllegalArgumentException("Cannot join your own game as other player");
        }

        game.setBlackPlayerId(userId);
        game.setStatus(Game.GameStatus.ACTIVE);
        gameDAO.updateGame(game);

        return true;
    }

    public Game makeMove(String gameCode, int userId, String from, String to, String promotion)
            throws SQLException {

        Game game = gameDAO.findByCode(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }

        if (game.getStatus() != Game.GameStatus.ACTIVE) {
            throw new IllegalArgumentException("Game is not active");
        }

        String userColor = game.isWhite(userId) ? "WHITE" :
                game.isBlack(userId) ? "BLACK" : null;

        if (userColor == null) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        if (!userColor.equals(game.getTurn())) {
            throw new IllegalArgumentException("Not your turn");
        }

        if (from == null || to == null || from.length() != 2 || to.length() != 2) {
            throw new IllegalArgumentException("Invalid squares: from=" + from + " to=" + to);
        }

        Board board = new Board();
        board.loadFromFen(game.getFenPosition());

        String uciMove = from.toLowerCase() + to.toLowerCase();
        if (promotion != null && !promotion.isEmpty()) {
            uciMove += promotion.toLowerCase();
        }

        try {
            boolean moved = board.doMove(uciMove);
            if (!moved) {
                throw new IllegalArgumentException("Illegal move");
            }

            Square fromSquare = Square.fromValue(from.toUpperCase());
            Square toSquare = Square.fromValue(to.toUpperCase());
            Piece promoPiece = Piece.NONE;
            if (promotion != null && !promotion.isEmpty()) {
                promoPiece = Piece.fromFenSymbol(promotion.toLowerCase());
            }

            com.github.bhlangonijr.chesslib.move.Move move =
                    new com.github.bhlangonijr.chesslib.move.Move(fromSquare, toSquare, promoPiece);

            game.setFenPosition(board.getFen());
            game.setTurn(game.getTurn().equals("WHITE") ? "BLACK" : "WHITE");
            game.setLastMoveAt(LocalDateTime.now());

            handleGameEnd(board, game);

            saveMoveToDatabase(game, from, to, promotion, move, board);
            gameDAO.updateGame(game);

            return game;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid move: " + e.getMessage());
        }
    }


    private void handleGameEnd(Board board, Game game) throws SQLException {
        if (board.isMated()) {
            game.setStatus(Game.GameStatus.FINISHED);
            game.setResult(game.getTurn().equals("WHITE") ?
                    Game.GameResult.BLACK_WIN : Game.GameResult.WHITE_WIN);

            updatePlayerStats(game);
        } else if (board.isDraw() || board.isStaleMate() || board.isInsufficientMaterial()) {
            game.setStatus(Game.GameStatus.FINISHED);
            game.setResult(Game.GameResult.DRAW);

            updatePlayerStatsForDraw(game);
        }
    }

    private void updatePlayerStats(Game game) throws SQLException {
        if (game.getResult() == Game.GameResult.WHITE_WIN) {
            userDAO.updateStats(game.getWhitePlayerId(), "WIN");
            userDAO.updateStats(game.getBlackPlayerId(), "LOSS");
        } else if (game.getResult() == Game.GameResult.BLACK_WIN) {
            userDAO.updateStats(game.getWhitePlayerId(), "LOSS");
            userDAO.updateStats(game.getBlackPlayerId(), "WIN");
        }
    }

    private void updatePlayerStatsForDraw(Game game) throws SQLException {
        userDAO.updateStats(game.getWhitePlayerId(), "DRAW");
        userDAO.updateStats(game.getBlackPlayerId(), "DRAW");
    }

    private void saveMoveToDatabase(Game game, String from, String to, String promotion,
                                    com.github.bhlangonijr.chesslib.move.Move move, Board board)
            throws SQLException {
        Move dbMove = new Move();
        dbMove.setGameId(game.getGameId());
        dbMove.setMoveNumber(moveDAO.getNextMoveNumber(game.getGameId()));
        dbMove.setFromSquare(from);
        dbMove.setToSquare(to);

        Square toSquare = Square.fromValue(to.toUpperCase());
        Piece piece = board.getPiece(toSquare);
        dbMove.setPiece(piece.getFenSymbol());
        dbMove.setSanNotation(move.getSan());

        Piece capturedPiece = board.getPiece(move.getTo());
        if (capturedPiece != Piece.NONE) {
            dbMove.setCapturedPiece(capturedPiece.getFenSymbol());
        }

        if (promotion != null && !promotion.isEmpty()) {
            dbMove.setPromotion(promotion.toLowerCase());
        }

        moveDAO.saveMove(dbMove);
    }

    public void resignGame(String gameCode, int userId) throws SQLException {
        Game game = gameDAO.findByCode(gameCode);

        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }

        if (!game.isWhite(userId) && !game.isBlack(userId)) {
            throw new IllegalArgumentException("You are not a player in this game");
        }

        game.setStatus(Game.GameStatus.FINISHED);

        if (game.isWhite(userId)) {
            game.setResult(Game.GameResult.BLACK_WIN);
            userDAO.updateStats(game.getWhitePlayerId(), "LOSS");
            userDAO.updateStats(game.getBlackPlayerId(), "WIN");
        } else {
            game.setResult(Game.GameResult.WHITE_WIN);
            userDAO.updateStats(game.getWhitePlayerId(), "WIN");
            userDAO.updateStats(game.getBlackPlayerId(), "LOSS");
        }

        gameDAO.updateGame(game);
    }
    public String getWinnerName(String gameCode) throws SQLException {
        Game game = gameDAO.findByCode(gameCode);

        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if(game.getStatus() != Game.GameStatus.FINISHED) {
            return "";
        }
        Game.GameResult res = game.getResult();
        if(res == Game.GameResult.WHITE_WIN) {
            return game.getWhitePlayerName();
        } else if(res == Game.GameResult.BLACK_WIN) {
            return game.getBlackPlayerName();
        } else {
            return "DRAW";
        }
    }

    public String getLoserName(String gameCode) throws SQLException {
        Game game = gameDAO.findByCode(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if(game.getStatus() != Game.GameStatus.FINISHED) {
            return "";
        }
        String winnerName = getWinnerName(gameCode);
        if(winnerName.equals(game.getWhitePlayerName())) {
            return game.getBlackPlayerName();
        } else if(winnerName.equals(game.getBlackPlayerName())) {
            return game.getWhitePlayerName();
        }
        return "";
    }

    public JSONObject getGameState(String gameCode) throws SQLException {
        Game game = gameDAO.findByCode(gameCode);

        if (game == null) {
            return null;
        }

        JSONObject gameState = new JSONObject();
        gameState.put("gameCode", game.getGameCode());
        gameState.put("fen", game.getFenPosition());
        gameState.put("turn", game.getTurn());
        gameState.put("status", game.getStatus().toString());
        gameState.put("whitePlayer", game.getWhitePlayerName());
        gameState.put("blackPlayer", game.getBlackPlayerName());

        if (game.getResult() != null) {
            gameState.put("result", game.getResult().toString());
        }

        return gameState;
    }

    public List<Game> listGamesForUser(int userId) throws SQLException {
        return gameDAO.findByUser(userId);
    }
}