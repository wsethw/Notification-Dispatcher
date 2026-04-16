const redisClient = require('./redisClient');

const PUSH_CHANNEL = 'channel:push:request';

function isValidUserId(userId) {
  return typeof userId === 'string' && userId.trim().length > 0;
}

function normalizeNotification(message) {
  const parsed = typeof message === 'string' ? JSON.parse(message) : message;

  if (!parsed || typeof parsed !== 'object') {
    throw new Error('Notification payload must be an object');
  }

  if (!isValidUserId(parsed.userId)) {
    throw new Error('Notification payload must include a valid userId');
  }

  return {
    notificationId: parsed.notificationId || null,
    userId: parsed.userId.trim(),
    subject: parsed.subject || 'Notification',
    body: parsed.body || '',
    channel: parsed.channel || 'push',
    timestamp: parsed.timestamp || new Date().toISOString()
  };
}

function setupSocketHandlers(io, options = {}) {
  const subscriber = options.subscriber || redisClient.duplicate();
  const pushChannel = options.pushChannel || PUSH_CHANNEL;

  subscriber.subscribe(pushChannel, (error) => {
    if (error) {
      console.error(`Failed to subscribe to ${pushChannel}`, error);
      return;
    }

    console.log(`Subscribed to ${pushChannel}`);
  });

  subscriber.on('message', (channel, message) => {
    try {
      const notification = normalizeNotification(message);
      io.to(`user:${notification.userId}`).emit('notification', notification);
      console.log(`Delivered notification ${notification.notificationId || 'without-id'} to user:${notification.userId}`);
    } catch (error) {
      console.error(`Error processing message from ${channel}:`, error);
    }
  });

  io.on('connection', (socket) => {
    console.log(`Client connected: ${socket.id}`);

    socket.on('subscribe', (payload = {}) => {
      const userId = typeof payload.userId === 'string' ? payload.userId.trim() : '';

      if (!isValidUserId(userId)) {
        socket.emit('subscription_error', {
          error: 'userId is required to subscribe to notifications'
        });
        return;
      }

      socket.join(`user:${userId}`);
      socket.emit('subscribed', {
        userId,
        message: 'Successfully subscribed to notifications'
      });
    });

    socket.on('disconnect', () => {
      console.log(`Client disconnected: ${socket.id}`);
    });
  });

  return subscriber;
}

module.exports = {
  PUSH_CHANNEL,
  isValidUserId,
  normalizeNotification,
  setupSocketHandlers
};
