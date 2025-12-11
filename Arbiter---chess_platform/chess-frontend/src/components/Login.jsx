import { useState } from 'react';
import { authAPI } from '../services/api.js';

function Login({ onLogin }) {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const data = isRegister
        ? await authAPI.register(username, password)
        : await authAPI.login(username, password);

      if (data.token) {
        onLogin(data.token, username);
      } else {
        console.log(data)
        setError(data.error || 'Authentication failed');
      }
    } catch (err) {
      setError('Connection error. Make sure backend is running.');
      console.log(err)
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>Arbiter - chess platform</h1>
        <h3>{isRegister ? 'Create Account' : 'Login to Play'}</h3>
        
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter username"
              required
              disabled={loading}
            />
          </div>
          
          <div className="input-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
              required
              disabled={loading}
            />
          </div>
          
          {error && <div className="error-message">⚠️ {error}</div>}
          
          <button 
            type="submit" 
            className="submit-btn"
            disabled={loading}
          >
            {loading ? 'Loading...' : (isRegister ? 'Register' : 'Login')}
          </button>
        </form>
        
        <div className="toggle-text">
          {isRegister ? 'Already have an account?' : 'New to Arbiter?'}
          <button 
            onClick={() => setIsRegister(!isRegister)}
            className="toggle-btn"
            disabled={loading}
          >
            {isRegister ? 'Login' : 'Register'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default Login;