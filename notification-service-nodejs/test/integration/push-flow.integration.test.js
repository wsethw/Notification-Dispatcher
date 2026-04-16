const EventEmitter = require('events');
const { io: clientIo } = require('socket.io-client');
const { createServer, shutdown } = require('../../src/index');

class IntegrationSubscriber extends EventEmitter {
  subscribe(channel, callback) {
    if (typeof callback === 'function') {
      callback(null, 1);
    }
    return Promise.resolve(1);
  }

  unsubscribe() {
    return Promise.resolve(0);
  }

  quit() {
    return Promise.resolve('OK');
  }
}

describe('push flow integration', () => {
  let subscriber;
  let server;
  let socket;

  beforeEach(async () => {
    subscriber = new IntegrationSubscriber();
    const created = createServer({
      socketOptions: { subscriber }
    });

    server = created.server;
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  });

  afterEach(async () => {
    if (socket && socket.connected) {
      socket.disconnect();
    }

    await shutdown(server, subscriber);
  });

  it('connects, subscribes and receives a notification from the redis subscriber', async () => {
    const address = server.address();
    const userId = 'integration-user';

    socket = clientIo(`http://127.0.0.1:${address.port}`, {
      transports: ['websocket'],
      forceNew: true
    });

    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('subscription timeout')), 5000);

      socket.on('connect', () => {
        socket.emit('subscribe', { userId });
      });

      socket.on('subscribed', (payload) => {
        clearTimeout(timer);
        expect(payload.userId).toBe(userId);
        resolve();
      });
    });

    const notificationPromise = new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('notification timeout')), 5000);
      socket.on('notification', (payload) => {
        clearTimeout(timer);
        resolve(payload);
      });
    });

    subscriber.emit(
      'message',
      'channel:push:request',
      JSON.stringify({
        notificationId: 'notif-integration-1',
        userId,
        subject: 'Integration Test',
        body: 'Push notification delivered',
        channel: 'push'
      })
    );

    await expect(notificationPromise).resolves.toMatchObject({
      notificationId: 'notif-integration-1',
      userId,
      subject: 'Integration Test',
      body: 'Push notification delivered',
      channel: 'push'
    });
  });
});
