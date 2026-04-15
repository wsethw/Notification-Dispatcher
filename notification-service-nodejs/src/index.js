const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const path = require('path');
const { setupSocketHandlers } = require('./socketHandler');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

app.use(express.json());
app.use(express.static(path.join(__dirname, '../public')));

// Setup Socket.IO handlers and Redis subscription
setupSocketHandlers(io);

// Health endpoint
app.get('/api/v1/health', (req, res) => {
  res.json({ status: 'UP' });
});

// Serve client
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/client.html'));
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`🚀 Node.js Notification Service listening on port ${PORT}`);
});