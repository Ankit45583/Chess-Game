import { useState, useEffect, useRef } from 'react';
import { Chessboard } from 'react-chessboard';
import { Chess } from 'chess.js';
import { connectWebSocket, sendMove, disconnectWebSocket, sendResign } from '../services/websocket.js';

function ChessGame({ gameCode, token, onExit }) {
  const INITIAL_TIME = 10 * 60; 
  const [whiteTime, setWhiteTime] = useState(INITIAL_TIME);
  const [blackTime, setBlackTime] = useState(INITIAL_TIME);
  const [activeColor, setActiveColor] = useState(null);
  const [game] = useState(() => new Chess());
  const [fen, setFen] = useState(game.fen());
  const [status, setStatus] = useState("waiting");
  const [whitePlayer, setWhitePlayer] = useState("Waiting...");
  const [blackPlayer, setBlackPlayer] = useState("Waiting...");
  const [mySide, setMySide] = useState("SPECTATOR");
  const [gameResult, setGameResult] = useState(null);

  const socketRef = useRef(null);

  useEffect(() => {
    if (status !== "active") {
      return;
    }
    const interval = setInterval(() => {
      if (activeColor === "white") {
        setWhiteTime((t) => (t > 0 ? t - 1 : 0));
      } else if (activeColor === "black") {
        setBlackTime((t) => (t > 0 ? t - 1 : 0));
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [activeColor, status]);

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  const handleServerMessage = (data) => {
    console.log("WS message:", data);

    if (data.type === "GAME_UPDATE") {
      const payload = data.data || data.payload || data;

      if (data.yourSide) {
        setMySide(data.yourSide);
      } else if (payload.yourSide) {
        setMySide(payload.yourSide);
      }

      if (payload.fen) {
        try {
          game.load(payload.fen);
          setFen(payload.fen);
        } catch (e) {
          console.warn("Invalid FEN from server:", payload.fen, e);
        }
      }

      if (payload.status === "ACTIVE" && payload.turn) {
        setActiveColor(payload.turn.toLowerCase()); 
      }

      if (payload.status) {
        setStatus(payload.status.toLowerCase());
      }

      if (payload.whitePlayer) {
        setWhitePlayer(payload.whitePlayer);
      }

      if (payload.blackPlayer) {
        setBlackPlayer(payload.blackPlayer);
      }
    } else if (
      data.type === "GAME_END" ||
      data.type === "PLAYER_LEFT" ||
      data.type === "PLAYER_DISCONNECTED"
    ) {
      const payload = data.data || data;
      setGameResult({
        winner: payload.winner ? payload.winner + " won" : "you won",
        reason: payload.reason || "Game finished",
        loser: payload.loser,
      });
      setStatus("finished");
    } else if (data.type === "CONNECTED" || data.type === "PLAYER_JOINED") {
        ;
    }
  };

  useEffect(() => {
    socketRef.current = connectWebSocket(gameCode, token, handleServerMessage);

    return () => {
      if (socketRef.current) {
        disconnectWebSocket(socketRef.current);
      }
    };
  }, [gameCode, token]);

  const handleDrop = ({ sourceSquare, targetSquare }) => {
    const piece = game.get(sourceSquare);
    if (!piece) return;

    const myColor = mySide === "WHITE" ? "w" : mySide === "BLACK" ? "b" : null;
    if (!myColor) return; 

    if (piece.color !== myColor) return;

    const gameTurn = game.turn();
    if (
      (gameTurn === "w" && myColor !== "w") ||
      (gameTurn === "b" && myColor !== "b")
    ) {
      return;
    }

    let promotion = null;
    if(piece.type === 'p' && piece.color === 'w' && targetSquare[1] == '8') {
        promotion = 'q';
    }
    if(piece.type === 'p' && piece.color === 'b' && targetSquare[1] == '1') {
        promotion = 'q';
    }
        
    try {
        const move = game.move({
          from: sourceSquare,
          to: targetSquare,
          promotion,
        });
        console.log(move);
        if (move === null) {
          return false;
        }
    }catch(err) {
        console.log(err.message)
        return false;
    }
        


    sendMove(gameCode, sourceSquare.toUpperCase(), targetSquare.toUpperCase(), promotion);

    setFen(game.fen());
  };

  const chessboardOptions = {
    id: "mainBoard",
    position: fen,
    onPieceDrop: handleDrop,
    boardOrientation: mySide === "BLACK" ? "black" : "white",
  };

  const handleResign = () => {
    if (window.confirm("Are you sure you want to resign?")) {
      sendResign(gameCode);
    }
  };

 return (
   <div className="game-room">
     <div className="game-header">
       <div className="game-code">
         <strong>Game:</strong> {gameCode}
       </div>
       <div className="game-controls">
         <button onClick={handleResign} className="resign-btn">
           Resign
         </button>
         <button onClick={onExit} className="exit-btn">
           Exit
         </button>
       </div>
     </div>

     <div className="game-status">
       Status:{" "}
       <span className="status-text">
         {status ? status.toString().toUpperCase() : "UNKNOWN"}
       </span>
       {gameResult && (
         <div className="game-result-banner">
           <div className="game-result-title">{gameResult.winner}</div>
         </div>
       )}
     </div>

     {mySide === "BLACK" ? (
       <>
         <div className="player">
           <div className="player-label">
             WHITE {mySide === "WHITE" ? "(You)" : ""}
           </div>
           <div className="player-main-row">
             <div className="player-name">{whitePlayer}</div>
             <div className="player-clock">{formatTime(whiteTime)}</div>
           </div>
         </div>

         <div className="board-container">
           <Chessboard options={chessboardOptions} />
         </div>

         <div className="player">
           <div className="player-label">
             BLACK {mySide === "BLACK" ? "(You)" : ""}
           </div>
           <div className="player-main-row">
             <div className="player-name">{blackPlayer}</div>
             <div className="player-clock">{formatTime(blackTime)}</div>
           </div>
         </div>
       </>
     ) : (
       <>
         <div className="player">
           <div className="player-label">
             BLACK {mySide === "BLACK" ? "(You)" : ""}
           </div>
           <div className="player-main-row">
             <div className="player-name">{blackPlayer}</div>
             <div className="player-clock">{formatTime(blackTime)}</div>
           </div>
         </div>

         <div className="board-container">
           <Chessboard options={chessboardOptions} />
         </div>

         <div className="player">
           <div className="player-label">
             WHITE {mySide === "WHITE" ? "(You)" : ""}
           </div>
           <div className="player-main-row">
             <div className="player-name">{whitePlayer}</div>
             <div className="player-clock">{formatTime(whiteTime)}</div>
           </div>
         </div>
       </>
     )}
   </div>
 );
}

export default ChessGame;