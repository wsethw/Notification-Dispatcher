describe('redis client configuration', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('uses localhost-friendly defaults outside production', () => {
    process.env.NODE_ENV = 'development';
    delete process.env.REDIS_HOST;
    delete process.env.REDIS_PORT;

    const { buildRedisOptions } = require('../../src/redisClient');
    const options = buildRedisOptions();

    expect(options.host).toBe('127.0.0.1');
    expect(options.port).toBe(6379);
    expect(options.lazyConnect).toBe(false);
  });

  it('keeps lazy connections enabled in tests', () => {
    process.env.NODE_ENV = 'test';
    process.env.REDIS_HOST = 'redis-test';
    process.env.REDIS_PORT = '6380';

    const { buildRedisOptions } = require('../../src/redisClient');
    const options = buildRedisOptions();

    expect(options.host).toBe('redis-test');
    expect(options.port).toBe(6380);
    expect(options.lazyConnect).toBe(true);
  });

  it('falls back to the default port when REDIS_PORT is invalid', () => {
    process.env.NODE_ENV = 'development';
    process.env.REDIS_PORT = 'not-a-number';

    const { buildRedisOptions } = require('../../src/redisClient');
    const options = buildRedisOptions();

    expect(options.port).toBe(6379);
  });

  it('caps retry delays to avoid noisy reconnect storms', () => {
    const { buildRedisOptions } = require('../../src/redisClient');
    const options = buildRedisOptions();

    expect(options.retryStrategy(1)).toBe(50);
    expect(options.retryStrategy(10)).toBe(500);
    expect(options.retryStrategy(100)).toBe(2000);
  });
});
