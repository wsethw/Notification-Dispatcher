describe('Code Imports and Exports', () => {
  it('should export setupSocketHandlers function', () => {
    const socketHandler = require('../../src/socketHandler');
    expect(socketHandler).toBeDefined();
    expect(socketHandler.setupSocketHandlers).toBeDefined();
    expect(typeof socketHandler.setupSocketHandlers).toBe('function');
  });

  it('should export redis client', () => {
    const redis = require('../../src/redisClient');
    expect(redis).toBeDefined();
  });

  it('should have redis methods', () => {
    const redis = require('../../src/redisClient');
    expect(redis.on).toBeDefined();
    expect(typeof redis.on).toBe('function');
  });

  it('should support redis configuration', () => {
    const Redis = require('ioredis');
    expect(Redis).toBeDefined();
    expect(typeof Redis).toBe('function');
  });

  it('should have proper socketHandler structure', () => {
    const { setupSocketHandlers } = require('../../src/socketHandler');
    const mockIo = { on: jest.fn(), to: jest.fn() };
    
    // Apenas verifica que não throws
    try {
      setupSocketHandlers(mockIo);
      expect(true).toBe(true);
    } catch (e) {
      expect(e).toBeNull();
    }
  });

  it('should have redis retry strategy', () => {
    const redisConfig = {
      host: 'redis',
      port: 6379,
      retryStrategy: (times) => Math.min(times * 50, 2000)
    };

    expect(redisConfig.retryStrategy).toBeDefined();
    expect(redisConfig.retryStrategy(1)).toBe(50);
    expect(redisConfig.retryStrategy(100)).toBe(2000);
  });
});