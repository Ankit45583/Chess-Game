import { useEffect, useState } from 'react';
import { authAPI } from '../services/api.js';

function formatDate(str) {
  if (!str) return '-';
  const d = new Date(str);
  return d.toLocaleString();
}

function HistoryPage({ token, onBack }) {
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await authAPI.fetchUserGames(token);
        setGames(Array.isArray(data) ? data : []);
      } catch (e) {
        setError(e.message || 'Failed to load history');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [token]);

  return (
    <div className="lobby">
      <div className="lobby-header">
        <h2>Game history</h2>
        <button className="logout-btn" onClick={onBack}>
          Back
        </button>
      </div>

      {loading && <div className="message-box">Loading…</div>}
      {error && <div className="error-message">{error}</div>}

      {!loading && !error && games.length === 0 && (
        <div className="message-box">No games yet.</div>
      )}

      {!loading && games.length > 0 && (
        <div className="history-table-wrapper">
          <table className="history-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Status</th>
                <th>Result</th>
                <th>Created</th>
                <th>Last move</th>
              </tr>
            </thead>
            <tbody>
              {games.map((g) => (
                <tr key={g.gameCode}>
                  <td>{g.gameCode}</td>
                  <td>{g.status}</td>
                  <td>{g.result ? g.result + " won" : '-'}</td>
                  <td>{formatDate(g.createdAt)}</td>
                  <td>{formatDate(g.lastMoveAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default HistoryPage;
