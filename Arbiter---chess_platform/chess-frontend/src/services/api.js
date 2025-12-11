const API_BASE = 'http://localhost:8080/api';


export const authAPI = {
  login: async (username, password) => {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    return response.json();
  },

  register: async (username, password) => {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    return response.json();
  },

  createGame: async (token) => {
    const response = await fetch(`${API_BASE}/game/create`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    return response.json();
  },

  joinGame: async (gameCode, token) => {
    const response = await fetch(`${API_BASE}/game/join/${gameCode}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    return response.json();
  },
  fetchUserGames: async (token) => {
    const res = await fetch(`${API_BASE}/user/games`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    if (!res.ok) throw new Error("Failed to load games");
    return res.json();
  },
};