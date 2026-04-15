jest.mock('ioredis', () => {
  return jest.fn(() => ({
    on: jest.fn(),
    duplicate: jest.fn(() => ({
      subscribe: jest.fn(),
      on: jest.fn()
    })),
    publish: jest.fn(),
    quit: jest.fn()
  }));
});

process.env.NODE_ENV = 'test';
process.env.REDIS_HOST = 'redis';
process.env.REDIS_PORT = '6379';
process.env.PORT = '3000';