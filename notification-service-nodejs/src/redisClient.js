const Redis = require('ioredis');

function buildRedisOptions() {
  return {
    host: process.env.REDIS_HOST || 'redis',
    port: Number(process.env.REDIS_PORT || 6379),
    lazyConnect: process.env.NODE_ENV === 'test',
    enableReadyCheck: true,
    maxRetriesPerRequest: null,
    retryStrategy(times) {
      return Math.min(times * 50, 2000);
    }
  };
}

const redis = new Redis(buildRedisOptions());

redis.on('connect', () => {
  console.log(
    `[redis] connected to ${process.env.REDIS_HOST || 'redis'}:${process.env.REDIS_PORT || 6379}`
  );
});

redis.on('error', (error) => {
  console.error('[redis] connection error', error);
});

module.exports = redis;
module.exports.buildRedisOptions = buildRedisOptions;
