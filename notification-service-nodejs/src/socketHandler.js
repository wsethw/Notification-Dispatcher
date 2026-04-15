const redis = require('./redisClient');

const setupSocketHandlers = (io) => {
  // Subscribe to push channel
  const subscriber = redis.duplicate();

  subscriber.subscribe('channel:push:request', (err) => {
    if (err) {
      console.error('Failed to subscribe to channel:push:request', err);
    } else {
      console.log('✅ Subscribed to channel:push:request');
    }
  });

  subscriber.on('message', (channel, message) => {
    try {
      const notification = JSON.parse(message);
      const userId = notification.userId;

      console.log(`📨 Received push notification for userId: ${userId}`);

      // Emit to the specific user via Socket.IO
      io.to(`user:${userId}`).emit('notification', {
        notificationId: notification.notificationId,
        subject: notification.subject,
        body: notification.body,
        channel: notification.channel,
        timestamp: new Date().toISOString()
      });

      console.log(`✉️  Sent notification to user:${userId}`);
    } catch (error) {
      console.error('Error processing message:', error);
    }
  });

  io.on('connection', (socket) => {
    console.log(`👤 Client connected: ${socket.id}`);

    socket.on('subscribe', (data) => {
      const userId = data.userId;
      socket.join(`user:${userId}`);
      console.log(`👤 User ${userId} subscribed via Socket.IO (socket: ${socket.id})`);
      socket.emit('subscribed', { userId, message: 'Successfully subscribed to notifications' });
    });

    socket.on('disconnect', () => {
      console.log(`👤 Client disconnected: ${socket.id}`);
    });
  });
};

module.exports = { setupSocketHandlers };