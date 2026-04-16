const EventEmitter = require('events');
const {
  normalizeNotification,
  setupSocketHandlers
} = require('../../src/socketHandler');

class FakeSocket extends EventEmitter {
  constructor() {
    super();
    this.id = 'socket-test';
    this.join = jest.fn();
    this.emit = jest.fn();
  }
}

class FakeSubscriber extends EventEmitter {
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

describe('socketHandler', () => {
  it('normalizes a valid notification payload', () => {
    const notification = normalizeNotification(
      JSON.stringify({
        notificationId: 'notif-1',
        userId: ' user-1 ',
        subject: 'Test',
        body: 'Body',
        channel: 'push'
      })
    );

    expect(notification).toMatchObject({
      notificationId: 'notif-1',
      userId: 'user-1',
      subject: 'Test',
      body: 'Body',
      channel: 'push'
    });
    expect(notification.timestamp).toEqual(expect.any(String));
  });

  it('rejects notifications without a valid userId', () => {
    expect(() => normalizeNotification({ userId: '' })).toThrow(
      'Notification payload must include a valid userId'
    );
  });

  it('emits a subscription error when userId is missing', () => {
    const subscriber = new FakeSubscriber();
    const io = {
      on: jest.fn((event, handler) => {
        if (event === 'connection') {
          io.connectionHandler = handler;
        }
      }),
      to: jest.fn(() => ({ emit: jest.fn() }))
    };

    setupSocketHandlers(io, { subscriber });

    const socket = new FakeSocket();
    io.connectionHandler(socket);
    socket.listeners('subscribe')[0]({});

    expect(socket.join).not.toHaveBeenCalled();
    expect(socket.emit).toHaveBeenCalledWith('subscription_error', {
      error: 'userId is required to subscribe to notifications'
    });
  });

  it('delivers notifications to the expected room', () => {
    const subscriber = new FakeSubscriber();
    const emit = jest.fn();
    const io = {
      on: jest.fn(),
      to: jest.fn(() => ({ emit }))
    };

    setupSocketHandlers(io, { subscriber });

    subscriber.emit(
      'message',
      'channel:push:request',
      JSON.stringify({
        notificationId: 'notif-1',
        userId: 'user-42',
        subject: 'Hello',
        body: 'World',
        channel: 'push'
      })
    );

    expect(io.to).toHaveBeenCalledWith('user:user-42');
    expect(emit).toHaveBeenCalledWith(
      'notification',
      expect.objectContaining({
        notificationId: 'notif-1',
        userId: 'user-42',
        subject: 'Hello',
        body: 'World',
        channel: 'push'
      })
    );
  });
});
