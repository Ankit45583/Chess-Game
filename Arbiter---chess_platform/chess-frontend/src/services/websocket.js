let socket = null;

export const connectWebSocket = (gameCode, token, onMessage) => {
  socket = new WebSocket(`ws://localhost:8081/com/chess/${gameCode}?token=${encodeURIComponent(token)}`);

  socket.onopen = () => {
    console.log('Connected to game', gameCode);
  };

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      onMessage(data);
    } catch (error) {
      console.error('Error parsing message:', error);
    }
  };

  socket.onclose = () => {
    console.log('Disconnected from game');
  };

  socket.onerror = (error) => {
    console.error('WebSocket error:', error);
  };

  return socket;
};

export const sendMove = (gameCode, from, to, promotion = null) => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    const move = {
      type: 'MOVE',
      gameCode,
      from,
      to,
      promotion
    };
    socket.send(JSON.stringify(move));
  }
};

export const sendResign = (gameCode) => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    const msg = { type: 'RESIGN', gameCode };
    socket.send(JSON.stringify(msg));
  }
};


export const disconnectWebSocket = (ws) => {
  if (ws) {
    ws.close();
  }
};