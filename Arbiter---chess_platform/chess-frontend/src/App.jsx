import { useState, useEffect } from 'react';
import Login from './components/Login.jsx';
import Lobby from './components/Lobby.jsx';
import ChessGame from './components/ChessGame.jsx';
import HistoryPage from './components/HistoryPage.jsx';
import './styles.css';

function App() {
  const [token, setToken] = useState(localStorage.getItem('chess_token') || '');
  const [username, setUsername] = useState(localStorage.getItem('username') || '');
  const [page, setPage] = useState('login');
  const [gameCode, setGameCode] = useState('');

  useEffect(() => {
    if (token) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPage('lobby');
    }
  }, [token]);

  const handleLogin = (newToken, username) => {
    setToken(newToken);
    setUsername(username);
    localStorage.setItem('chess_token', newToken);
    localStorage.setItem('username', username);
    setPage('lobby');
  };

  const handleLogout = () => {
    setToken('');
    localStorage.removeItem('chess_token');
    setPage('login');
  };

  const handleCreateGame = (code) => {
    setGameCode(code);
    setPage('game');
  };

  const handleJoinGame = (code) => {
    setGameCode(code);
    setPage('game');
  };

  const handleExitGame = () => {
    setGameCode('');
    setPage('lobby');
  };

  return (
    <div className="app">
      {page === "login" && <Login onLogin={handleLogin} />}
      {page === "lobby" && (
        <Lobby
          token={token}
          onLogout={handleLogout}
          onCreateGame={handleCreateGame}
          onJoinGame={handleJoinGame}
          onShowHistory={() => setPage("history")}
        />
      )}
      {page === 'history' && (
        <HistoryPage
          token={token}
          onBack={() => setPage('lobby')}
        />
      )}
      {page === "game" && (
        <ChessGame
          gameCode={gameCode}
          token={token}
          onExit={handleExitGame}
          username={username}
        />
      )}
    </div>
  );
}

export default App;