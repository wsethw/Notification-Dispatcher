const redisClient = require('../../src/redisClient');

describe('redisClient', () => {
  it('builds redis options from environment variables', () => {
    expect(redisClient.buildRedisOptions()).toEqual({
      host: 'redis',
      port: 6379,
      lazyConnect: true,
      enableReadyCheck: true,
      maxRetriesPerRequest: null,
      retryStrategy: expect.any(Function)
    });
  });

  it('exports a redis-like client instance', () => {
    expect(redisClient).toBeDefined();
    expect(typeof redisClient.on).toBe('function');
    expect(typeof redisClient.duplicate).toBe('function');
  });
});
