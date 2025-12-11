CREATE DATABASE IF NOT EXISTS chess_db;
USE chess_db;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rating INT DEFAULT 1200,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS games (
    game_id INT AUTO_INCREMENT PRIMARY KEY,
    game_code VARCHAR(6) UNIQUE NOT NULL,
    white_player_id INT,
    black_player_id INT,
    fen_position VARCHAR(100) DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    status ENUM('WAITING', 'ACTIVE', 'FINISHED', 'ABANDONED') DEFAULT 'WAITING',
    result ENUM('WHITE_WIN', 'BLACK_WIN', 'DRAW', 'ABANDONED') DEFAULT NULL,
    turn ENUM('WHITE', 'BLACK') DEFAULT 'WHITE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_move_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (white_player_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (black_player_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_game_code (game_code),
    INDEX idx_status (status),
    INDEX idx_players (white_player_id, black_player_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS moves (
    move_id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    move_number INT NOT NULL,
    from_square VARCHAR(2) NOT NULL,
    to_square VARCHAR(2) NOT NULL,
    piece VARCHAR(1) NOT NULL,
    captured_piece VARCHAR(1) DEFAULT NULL,
    promotion VARCHAR(1) DEFAULT NULL,
    san_notation VARCHAR(10) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE,
    INDEX idx_game_moves (game_id, move_number),
    INDEX idx_game_move_numbers (game_id, move_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS active_sessions (
    session_id VARCHAR(100) PRIMARY KEY,
    user_id INT NOT NULL,
    game_code VARCHAR(6) DEFAULT NULL,
    ws_session_id VARCHAR(100) DEFAULT NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_sessions (user_id),
    INDEX idx_game_sessions (game_code),
    INDEX idx_last_activity (last_activity)
) ENGINE=InnoDB DEFAULT;

DELIMITER //
CREATE PROCEDURE cleanup_old_sessions()
BEGIN
    DELETE FROM active_sessions
    WHERE last_activity < DATE_SUB(NOW(), INTERVAL 1 DAY);
END //
DELIMITER ;