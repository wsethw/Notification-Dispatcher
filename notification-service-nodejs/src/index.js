const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const path = require('path');
const { setupSocketHandlers } = require('./socketHandler');

function buildApp() {
  const app = express();

  app.use(express.json());
  app.use(express.static(path.join(__dirname, '../public')));

  app.get('/api/v1/health', (req, res) => {
    res.json({
      status: 'UP',
      service: 'notification-service-nodejs'
    });
  });

  app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '../public/client.html'));
  });

  return app;
}

function createServer(options = {}) {
  const app = buildApp();
  const server = http.createServer(app);
  const io = socketIo(server, {
    cors: {
      origin: options.corsOrigin || '*',
      methods: ['GET', 'POST']
    }
  });

  const subscriber = setupSocketHandlers(io, options.socketOptions);

  return { app, server, io, subscriber };
}

async function shutdown(server, subscriber) {
  await new Promise((resolve) => server.close(resolve));

  if (subscriber && typeof subscriber.unsubscribe === 'function') {
    await subscriber.unsubscribe();
  }

  if (subscriber && typeof subscriber.quit === 'function') {
    await subscriber.quit();
  }
}

if (require.main === module) {
  const port = Number(process.env.PORT || 3000);
  const { server, subscriber } = createServer();

  server.listen(port, () => {
    console.log(`Node.js Notification Service listening on port ${port}`);
  });

  const handleSignal = async () => {
    try {
      await shutdown(server, subscriber);
    } finally {
      process.exit(0);
    }
  };

  process.on('SIGINT', handleSignal);
  process.on('SIGTERM', handleSignal);
}

module.exports = {
  buildApp,
  createServer,
  shutdown
};
