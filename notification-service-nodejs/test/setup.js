jest.mock('ioredis', () => {
  const { EventEmitter: MockEventEmitter } = require('events');

  class MockRedis extends MockEventEmitter {
    constructor(options = {}) {
      super();
      this.options = options;
      this.subscriptions = new Set();
    }

    subscribe(channel, callback) {
      this.subscriptions.add(channel);
      if (typeof callback === 'function') {
        callback(null, this.subscriptions.size);
      }
      return Promise.resolve(this.subscriptions.size);
    }

    unsubscribe() {
      this.subscriptions.clear();
      return Promise.resolve(0);
    }

    quit() {
      return Promise.resolve('OK');
    }

    duplicate() {
      return new MockRedis(this.options);
    }
  }

  return jest.fn((options) => new MockRedis(options));
});

process.env.NODE_ENV = 'test';
process.env.REDIS_HOST = 'redis';
process.env.REDIS_PORT = '6379';
process.env.PORT = '3000';

afterEach(() => {
  jest.clearAllMocks();
  jest.restoreAllMocks();
});
