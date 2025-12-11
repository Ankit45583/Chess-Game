import { useState } from 'react';
import { authAPI } from '../services/api.js';

function Lobby({ token, onLogout, onCreateGame, onJoinGame, onShowHistory }) {
  const [gameCode, setGameCode] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const username = localStorage.getItem('username') || 'Player';


  const handleCreateGame = async () => {
    setMessage('');
    setLoading(true);
    
    try {
      const data = await authAPI.createGame(token);
      if (data.gameCode) {
        onCreateGame(data.gameCode);
      } else {
        setMessage(data.message || 'Failed to create game');
      }
    } catch (err) {
      setMessage('Cannot connect to server');
      console.log(err)
    } finally {
      setLoading(false);
    }
  };

  const handleJoinGame = async () => {
    const code = gameCode.trim().toUpperCase();
    if (!code || code.length !== 6) {
      setMessage('Please enter a valid 6-digit code');
      return;
    }
    
    setMessage('');
    setLoading(true);
    
    try {
      const data = await authAPI.joinGame(code, token);
      if (data.success) {
        onJoinGame(code);
      } else {
        setMessage(data.message || 'Game not found');
      }
    } catch (err) {
      setMessage('Cannot connect to server');
      console.log(err)
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="lobby">
      <div className="lobby-header">
        <h2>Arbiter</h2>
        <div className="header-right">
          <div className="user-display">
            <span className="welcome-text">Welcome,</span>
            <span className="username-display">{username}</span>
          </div>
          <div className="lobby-actions">
          <button type='button' className="logout-btn" onClick={onShowHistory}>
            History
          </button>
          <button type='button' className="logout-btn" onClick={onLogout}>
            Logout
          </button>
        </div>
        </div>
      </div>
       
      <div className="lobby-content">
        <div className="action-card">
          <h3>Create New Game</h3>
          <p>Start a new game and share the code with a friend</p>
          <button type='button'
            onClick={handleCreateGame}
            disabled={loading}
            className="action-btn create-btn"
          >
            {loading ? 'Creating...' : 'Create Game'}
          </button>
        </div>
        
        <div className="divider">
          <span>OR</span>
        </div>
        
        <div className="action-card">
          <h3>Join Existing Game</h3>
          <p>Enter a 6-digit game code</p>
          <div className="join-section">
            <input
              type="text"
              value={gameCode}
              onChange={(e) => setGameCode(e.target.value.toUpperCase())}
              placeholder="123456"
              maxLength="6"
              disabled={loading}
              className="code-input"
            />
            <button 
              onClick={handleJoinGame}
              disabled={loading || !gameCode}
              className="action-btn join-btn"
            >
              {loading ? 'Joining...' : 'Join Game'}
            </button>
          </div>
        </div>
        
        {message && (
          <div className="message-box">
            {message}
          </div>
        )}
      </div>
    </div>
  );
}

export default Lobby;